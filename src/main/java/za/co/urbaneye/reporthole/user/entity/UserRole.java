package za.co.urbaneye.reporthole.user.entity;

/**
 * Enumeration representing the available user roles
 * within the ReportHole system.
 *
 * <p>Roles determine access levels and responsibilities
 * within the application.</p>
 *
 * <ul>
 *     <li>{@code CIVILIAN} - Standard public user who reports issues</li>
 *     <li>{@code CONTRACTOR} - User responsible for resolving assigned issues</li>
 *     <li>{@code ADMIN} - Administrative user with elevated privileges</li>
 * </ul>
 *
 * <p>This enum is typically persisted as a string value
 * in the database and used for authorization checks.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public enum UserRole {

    /**
     * Standard citizen user role.
     */
    CIVILIAN,

    /**
     * Contractor or service provider role.
     */
    CONTRACTOR,

    /**
     * Administrative role with elevated access.
     */
    ADMIN
}