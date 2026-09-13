package za.co.urbaneye.reporthole.admin.user.service.interfaces;

import za.co.urbaneye.reporthole.admin.user.dto.CivilianSummaryResponse;

import java.util.List;

/**
 * Service contract for admin-facing user management operations.
 *
 * @author Refentse
 * @since 1.0
 */
public interface IAdminUserService {

    /**
     * Returns a summary list of all civilian accounts, with names and emails partially masked.
     *
     * @return list of civilian summaries sorted by registration date, newest first
     */
    List<CivilianSummaryResponse> getCivilians();
}
