package za.co.urbaneye.reporthole.message.entity;

/**
 * Classifies the source and intended audience of a {@link Message}.
 *
 * <ul>
 *     <li>{@code CIVILIAN_COMPLAINT} — sent by an authenticated civilian from their dashboard
 *         to the municipal admin team.</li>
 *     <li>{@code CONTACT_US} — submitted via the public landing-page contact form, potentially
 *         by anyone (no JWT required). Visible only to security admins.</li>
 * </ul>
 *
 * @author Refentse
 * @since 1.0
 */
public enum MessageCategory {
    CIVILIAN_COMPLAINT,
    CONTACT_US
}
