package za.co.urbaneye.reporthole.inference.service;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.FloatBuffer;

/**
 * Converts raw image bytes into an ONNX input tensor compatible with the
 * exported Reporthole-v1 YOLOv8 model.
 *
 * <p>The model expects input with shape {@code [1, 3, 640, 640]} (NCHW),
 * float32, with pixel values normalised to [0.0, 1.0] (original pixel ÷ 255).
 * Images are letterbox-resized: the original aspect ratio is preserved by
 * scaling to fit within 640×640 and padding the remaining area with black.</p>
 *
 * <p>This class is stateless and all methods are static — it does not need
 * to be instantiated.</p>
 *
 * @author Refentse
 * @since 1.0
 */
public final class ImagePreprocessor {

    /** Target width and height expected by Reporthole-v1.onnx. */
    public static final int TARGET_SIZE = 640;

    private ImagePreprocessor() {}

    /**
     * Decodes the supplied image bytes and produces an ONNX tensor ready to
     * pass as the {@code "images"} input to the inference session.
     *
     * <p>Processing steps:</p>
     * <ol>
     *   <li>Decode bytes to a {@link BufferedImage} via {@link ImageIO}.</li>
     *   <li>Letterbox resize to {@value #TARGET_SIZE}×{@value #TARGET_SIZE}
     *       (scale by min ratio, centre the image on a black canvas).</li>
     *   <li>Extract RGB channels; build a CHW float array normalised to [0, 1].</li>
     *   <li>Wrap in an {@link OnnxTensor} with shape
     *       {@code [1, 3, TARGET_SIZE, TARGET_SIZE]}.</li>
     * </ol>
     *
     * @param imageBytes raw bytes of a JPEG or PNG image
     * @param env        the shared {@link OrtEnvironment} used to allocate the tensor
     * @return an {@link OnnxTensor} of shape {@code [1, 3, 640, 640]}
     * @throws IOException  if the bytes cannot be decoded as an image
     * @throws OrtException if tensor allocation fails
     */
    public static OnnxTensor preprocess(byte[] imageBytes, OrtEnvironment env)
            throws IOException, OrtException {

        BufferedImage original = ImageIO.read(new ByteArrayInputStream(imageBytes));
        if (original == null) {
            throw new IOException("Could not decode image — unsupported format or corrupt data");
        }

        BufferedImage letterboxed = letterbox(original, TARGET_SIZE);
        float[] chw = toChwFloat(letterboxed);

        FloatBuffer buffer = FloatBuffer.wrap(chw);
        return OnnxTensor.createTensor(env, buffer, new long[]{1, 3, TARGET_SIZE, TARGET_SIZE});
    }

    /**
     * Scales the image to fit within a {@code size × size} square while preserving
     * aspect ratio, then centres it on a black canvas of exactly {@code size × size}.
     *
     * @param src  source image
     * @param size target canvas dimension
     * @return letterboxed {@link BufferedImage} with dimensions {@code size × size}
     */
    public static BufferedImage letterbox(BufferedImage src, int size) {
        int srcW = src.getWidth();
        int srcH = src.getHeight();

        double scale = Math.min((double) size / srcW, (double) size / srcH);
        int scaledW = (int) Math.round(srcW * scale);
        int scaledH = (int) Math.round(srcH * scale);

        int padLeft = (size - scaledW) / 2;
        int padTop  = (size - scaledH) / 2;

        BufferedImage canvas = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = canvas.createGraphics();
        g.setColor(Color.BLACK);
        g.fillRect(0, 0, size, size);
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.drawImage(src, padLeft, padTop, scaledW, scaledH, null);
        g.dispose();

        return canvas;
    }

    /**
     * Converts a {@code TYPE_INT_RGB} image to a flat float array in CHW order,
     * with pixel values normalised to [0.0, 1.0] by dividing by 255.
     *
     * <p>Array layout: all red channel values first (row-major), then green, then blue.
     * Index formula: {@code channelOffset + row * width + col} where
     * {@code channelOffset} is 0, {@code width*height}, or {@code 2*width*height}
     * for R, G, B respectively.</p>
     *
     * @param img source image (must be exactly {@value #TARGET_SIZE}×{@value #TARGET_SIZE})
     * @return float array of length {@code 3 * width * height}
     */
    public static float[] toChwFloat(BufferedImage img) {
        int w = img.getWidth();
        int h = img.getHeight();
        int pixels = w * h;
        float[] chw = new float[3 * pixels];

        int[] rgbArray = img.getRGB(0, 0, w, h, null, 0, w);
        for (int i = 0; i < pixels; i++) {
            int rgb = rgbArray[i];
            chw[i]              = ((rgb >> 16) & 0xFF) / 255.0f; // R
            chw[pixels + i]     = ((rgb >>  8) & 0xFF) / 255.0f; // G
            chw[2 * pixels + i] = ( rgb        & 0xFF) / 255.0f; // B
        }
        return chw;
    }
}
