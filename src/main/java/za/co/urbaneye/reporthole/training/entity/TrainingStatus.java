package za.co.urbaneye.reporthole.training.entity;

/** Where an incident's image sits in the YOLO retraining pipeline. Independent of the incident workflow status. */
public enum TrainingStatus {
    NOT_FLAGGED,
    FLAGGED,
    /** Included in a YOLO export; re-flagging moves it back to {@link #FLAGGED} for a later export. */
    EXPORTED
}
