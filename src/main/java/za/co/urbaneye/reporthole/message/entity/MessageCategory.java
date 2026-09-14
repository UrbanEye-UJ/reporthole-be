package za.co.urbaneye.reporthole.message.entity;

/**
 * Classifies the source and intended audience of a {@link Message}.
 *
 * <ul>
 *     <li>{@code USER_MESSAGE} — sent by any authenticated user (civilian, contractor, or
 *         admin) to the municipal/security admin team. See {@link Message#getSenderRole()}
 *         for who specifically sent it.</li>
 *     <li>{@code CONTACT_US} — submitted via the public landing-page contact form, potentially
 *         by anyone (no JWT required). Visible only to security admins.</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public enum MessageCategory {
    USER_MESSAGE,
    CONTACT_US
}
