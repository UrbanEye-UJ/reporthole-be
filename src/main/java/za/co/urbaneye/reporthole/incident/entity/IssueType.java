package za.co.urbaneye.reporthole.incident.entity;

public enum IssueType {
    POTHOLE,
    CRACK,
    FADED_MARKINGS,
    DAMAGED_SIGN,
    BLOCKED_DRAIN,
    BROKEN_TRAFFIC_LIGHT,
    ACCIDENT,
    /** Catch-all — a contractor with this specialisation can be assigned to any incident type. */
    OTHER
}
