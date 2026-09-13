package za.co.urbaneye.reporthole.admin.security.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.security.dto.AuditEntryResponse;
import za.co.urbaneye.reporthole.admin.security.dto.GrantRoleRequest;
import za.co.urbaneye.reporthole.admin.security.dto.SecurityUserResponse;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAction;
import za.co.urbaneye.reporthole.admin.security.entity.AccessControlAuditEntry;
import za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException;
import za.co.urbaneye.reporthole.admin.security.repository.IAccessControlAuditRepository;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.ISecurityAdminService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserAuth;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.entity.UserStatus;
import za.co.urbaneye.reporthole.user.repository.IUserAuthRepository;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Default {@link ISecurityAdminService}.
 *
 * <p>Enforces separation of duties: the caller must hold {@code SECURITY_ADMIN}, and no method
 * lets them touch their own account. Every mutation records one append-only audit row and, where
 * the effect must be immediate, bumps the target's {@code credentialsValidFrom} watermark so the
 * {@link za.co.urbaneye.reporthole.security.JwtAuthenticationFilter} rejects their existing
 * tokens on the very next request.</p>
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SecurityAdminServiceImpl implements ISecurityAdminService {

    private final IUserRepository userRepository;
    private final IUserAuthRepository userAuthRepository;
    private final IAccessControlAuditRepository auditRepository;

    @Override
    @Transactional
    public void grantRole(UUID targetUserId, GrantRoleRequest request) {
        User actor = requireSecurityAdmin();
        User target = loadTarget(targetUserId);
        rejectSelfTarget(actor, target, "change your own role");

        UserRole previous = target.getRole();
        if (previous == request.role()) {
            throw new SecurityAdminException("Account already has role " + request.role());
        }

        target.setRole(request.role());
        userRepository.save(target);
        bumpCredentials(targetUserId);

        writeAudit(AccessControlAction.ROLE_GRANTED, actor, target,
                previous.name(), request.role().name(), request.reason());
        log.info("SECURITY_ADMIN {} changed role of {} from {} to {}",
                actor.getUserId(), targetUserId, previous, request.role());
    }

    @Override
    @Transactional
    public void revokeRole(UUID targetUserId, String reason) {
        User actor = requireSecurityAdmin();
        User target = loadTarget(targetUserId);
        rejectSelfTarget(actor, target, "revoke your own role");

        UserRole previous = target.getRole();
        if (previous == UserRole.CIVILIAN) {
            throw new SecurityAdminException("Account has no elevated role to revoke");
        }

        target.setRole(UserRole.CIVILIAN);
        userRepository.save(target);
        bumpCredentials(targetUserId);

        writeAudit(AccessControlAction.ROLE_REVOKED, actor, target,
                previous.name(), UserRole.CIVILIAN.name(), reason);
        log.info("SECURITY_ADMIN {} revoked role {} from {}", actor.getUserId(), previous, targetUserId);
    }

    @Override
    @Transactional
    public void suspendAccount(UUID targetUserId, String reason) {
        User actor = requireSecurityAdmin();
        User target = loadTarget(targetUserId);
        rejectSelfTarget(actor, target, "suspend your own account");

        UserAuth auth = loadAuth(targetUserId);
        if (auth.getStatus() == UserStatus.SUSPENDED) {
            throw new SecurityAdminException("Account is already suspended");
        }

        UserStatus previous = auth.getStatus();
        auth.setStatus(UserStatus.SUSPENDED);
        auth.setCredentialsValidFrom(nowToSeconds());
        userAuthRepository.save(auth);

        writeAudit(AccessControlAction.ACCOUNT_SUSPENDED, actor, target,
                previous.name(), UserStatus.SUSPENDED.name(), reason);
        log.info("SECURITY_ADMIN {} suspended account {} (was {})", actor.getUserId(), targetUserId, previous);
    }

    @Override
    @Transactional
    public void reactivateAccount(UUID targetUserId, String reason) {
        User actor = requireSecurityAdmin();
        User target = loadTarget(targetUserId);

        UserAuth auth = loadAuth(targetUserId);
        if (auth.getStatus() != UserStatus.SUSPENDED) {
            throw new SecurityAdminException("Account is not suspended");
        }

        auth.setStatus(UserStatus.ACTIVE);
        userAuthRepository.save(auth);

        writeAudit(AccessControlAction.ACCOUNT_REACTIVATED, actor, target,
                UserStatus.SUSPENDED.name(), UserStatus.ACTIVE.name(), reason);
        log.info("SECURITY_ADMIN {} reactivated account {}", actor.getUserId(), targetUserId);
    }

    @Override
    @Transactional
    public void forceLogout(UUID targetUserId, String reason) {
        User actor = requireSecurityAdmin();
        User target = loadTarget(targetUserId);
        rejectSelfTarget(actor, target, "force-logout your own account");

        bumpCredentials(targetUserId);

        writeAudit(AccessControlAction.SESSIONS_REVOKED, actor, target, null, null, reason);
        log.info("SECURITY_ADMIN {} revoked all sessions for account {}", actor.getUserId(), targetUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SecurityUserResponse> listUsers() {
        requireSecurityAdmin();

        Map<UUID, UserAuth> authById = userAuthRepository.findAll().stream()
                .collect(Collectors.toMap(UserAuth::getAuthId, Function.identity()));

        return userRepository.findAll().stream()
                .map(user -> {
                    UserAuth auth = authById.get(user.getUserId());
                    return new SecurityUserResponse(
                            user.getUserId(),
                            (user.getFirstName() + " " + user.getLastName()).trim(),
                            auth != null ? auth.getEmail() : null,
                            user.getRole(),
                            auth != null ? auth.getStatus() : null,
                            user.getCreatedAt());
                })
                .sorted((a, b) -> {
                    if (a.createdAt() == null || b.createdAt() == null) return 0;
                    return b.createdAt().compareTo(a.createdAt());
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditEntryResponse> listAudit(UUID targetUserId) {
        requireSecurityAdmin();

        List<AccessControlAuditEntry> entries = targetUserId == null
                ? auditRepository.findAllByOrderByCreatedAtDesc()
                : auditRepository.findByTarget_UserIdOrderByCreatedAtDesc(targetUserId);

        return entries.stream().map(AuditEntryResponse::from).toList();
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    /**
     * Loads the calling account and asserts it holds {@code SECURITY_ADMIN}.
     *
     * @return the caller's {@link User}
     * @throws SecurityAdminException if the caller is unknown or not a security admin
     */
    private User requireSecurityAdmin() {
        User caller = userRepository.findById(currentUserId())
                .orElseThrow(() -> new SecurityAdminException("User not found"));
        if (caller.getRole() != UserRole.SECURITY_ADMIN) {
            throw new SecurityAdminException("Only security admins can perform this action");
        }
        return caller;
    }

    /**
     * @param targetUserId the id from the path
     * @return the target {@link User}
     * @throws SecurityAdminException if no such user exists
     */
    private User loadTarget(UUID targetUserId) {
        return userRepository.findById(targetUserId)
                .orElseThrow(() -> new SecurityAdminException("Target user not found"));
    }

    /**
     * @param targetUserId the id from the path
     * @return the target {@link UserAuth}
     * @throws SecurityAdminException if no such auth record exists
     */
    private UserAuth loadAuth(UUID targetUserId) {
        return userAuthRepository.findById(targetUserId)
                .orElseThrow(() -> new SecurityAdminException("Target user not found"));
    }

    /**
     * Guards against a security admin acting on their own account.
     *
     * @param actor  the caller
     * @param target the account being acted on
     * @param what   phrase describing the blocked action, for the error message
     * @throws SecurityAdminException if {@code actor} and {@code target} are the same account
     */
    private void rejectSelfTarget(User actor, User target, String what) {
        if (actor.getUserId().equals(target.getUserId())) {
            throw new SecurityAdminException("A security admin cannot " + what);
        }
    }

    /**
     * Bumps the target account's {@code credentialsValidFrom} watermark to now, invalidating
     * every JWT issued before this second.
     *
     * @param targetUserId the account whose sessions should be cut off
     */
    private void bumpCredentials(UUID targetUserId) {
        UserAuth auth = loadAuth(targetUserId);
        auth.setCredentialsValidFrom(nowToSeconds());
        userAuthRepository.save(auth);
    }

    /**
     * Appends one row to the append-only access-control audit trail.
     *
     * @param action what happened
     * @param actor  the security admin who acted
     * @param target the affected account
     * @param from   previous value, or null
     * @param to     new value, or null
     * @param reason the supplied justification
     */
    private void writeAudit(AccessControlAction action, User actor, User target,
                            String from, String to, String reason) {
        auditRepository.save(AccessControlAuditEntry.builder()
                .action(action)
                .actor(actor)
                .target(target)
                .fromValue(from)
                .toValue(to)
                .reason(reason)
                .build());
    }

    /**
     * @return the current instant truncated to whole seconds, matching JWT {@code iat} precision
     */
    private LocalDateTime nowToSeconds() {
        return LocalDateTime.now().truncatedTo(ChronoUnit.SECONDS);
    }

    /**
     * @return the authenticated caller's user id, taken from the security context principal
     */
    private UUID currentUserId() {
        return UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()
        );
    }
}
