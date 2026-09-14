package za.co.urbaneye.reporthole.admin.security.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import za.co.urbaneye.reporthole.admin.security.dto.AuditLogEntryResponse;
import za.co.urbaneye.reporthole.admin.security.entity.AuditLogEntry;
import za.co.urbaneye.reporthole.admin.security.exception.SecurityAdminException;
import za.co.urbaneye.reporthole.admin.security.repository.IAuditLogRepository;
import za.co.urbaneye.reporthole.admin.security.service.interfaces.IAuditLogService;
import za.co.urbaneye.reporthole.user.entity.User;
import za.co.urbaneye.reporthole.user.entity.UserRole;
import za.co.urbaneye.reporthole.user.repository.IUserRepository;

import java.util.List;
import java.util.UUID;

/**
 * Default {@link IAuditLogService}.
 *
 * @author Refentse
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
public class AuditLogServiceImpl implements IAuditLogService {

    private final IAuditLogRepository repository;
    private final IUserRepository userRepository;

    @Override
    @Transactional
    public void record(User actor, String action, String entityType, UUID entityId, String summary) {
        repository.save(AuditLogEntry.builder()
                .actor(actor)
                .action(action)
                .entityType(entityType)
                .entityId(entityId)
                .summary(summary)
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuditLogEntryResponse> listAll() {
        requireSecurityAdmin();
        return repository.findAllByOrderByCreatedAtDesc().stream()
                .map(AuditLogEntryResponse::from)
                .toList();
    }

    private void requireSecurityAdmin() {
        UUID callerId = UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal());
        User caller = userRepository.findById(callerId)
                .orElseThrow(() -> new SecurityAdminException("User not found"));
        if (caller.getRole() != UserRole.SECURITY_ADMIN) {
            throw new SecurityAdminException("Only security admins can perform this action");
        }
    }
}
