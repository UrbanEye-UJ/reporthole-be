package za.co.urbaneye.reporthole.user.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import za.co.urbaneye.reporthole.user.entity.User;

/**
 * MapStruct mapper interface for converting user request DTOs
 * into {@link User} entity objects.
 *
 * <p>This mapper reduces boilerplate conversion code by generating
 * implementations automatically at build time.</p>
 *
 * <p>Configuration:</p>
 * <ul>
 *     <li>{@code componentModel = "spring"} - Registers mapper as a Spring bean</li>
 *     <li>{@code unmappedTargetPolicy = ReportingPolicy.ERROR} -
 *     Fails compilation when target fields are not explicitly handled</li>
 * </ul>
 *
 * <p>Supports mapping from:</p>
 * <ul>
 *     <li>{@link RegisterRequest} to {@link User}</li>
 *     <li>{@link LoginRequest} to {@link User}</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
public interface IUserMapper {

    /**
     * Converts a registration request into a new {@link User} entity.
     *
     * <p>The following fields are ignored because they are system-managed:</p>
     * <ul>
     *     <li>userId</li>
     *     <li>createdAt</li>
     * </ul>
     *
     * @param user registration request data
     * @return mapped user entity
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(RegisterRequest user);

    /**
     * Converts a login request into a partial {@link User} entity.
     *
     * <p>Only relevant login fields are mapped. Other entity fields are ignored
     * because they are not required during authentication.</p>
     *
     * @param user login request data
     * @return partially mapped user entity
     */
    @Mapping(target = "userId", ignore = true)
    @Mapping(target = "firstName", ignore = true)
    @Mapping(target = "lastName", ignore = true)
    @Mapping(target = "emailHash", ignore = true)
    @Mapping(target = "phoneNumber", ignore = true)
    @Mapping(target = "role", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    User toEntity(LoginRequest user);
}