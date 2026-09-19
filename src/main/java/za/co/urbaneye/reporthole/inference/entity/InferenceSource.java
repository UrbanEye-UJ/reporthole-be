package za.co.urbaneye.reporthole.inference.entity;

/**
 * Identifies which model produced an {@link InferenceResult}.
 *
 * @author Refentse
 * @since 1.0
 */
public enum InferenceSource {

    /** The Reporthole-v1 model, trained specifically on road-damage imagery. */
    CUSTOM,

    /** The stock, COCO-pretrained YOLOv8 model — generic object classes only. */
    STOCK
}
