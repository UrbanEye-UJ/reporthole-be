package za.co.urbaneye.reporthole.inference;

import org.junit.jupiter.api.Test;
import za.co.urbaneye.reporthole.inference.service.ImagePreprocessor;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.offset;

/**
 * Unit tests for {@link ImagePreprocessor}.
 *
 * <p>No ONNX Runtime session is needed — these tests verify the preprocessing
 * steps (letterboxing, channel layout, normalisation) without loading the model.</p>
 *
 * @author Refentse
 * @since 1.0
 */
class ImagePreprocessorTest {

    private static final int TARGET = ImagePreprocessor.TARGET_SIZE;

    // ── toChwFloat ──────────────────────────────────────────────────────────

    @Test
    void toChwFloat_producesCorrectLength() {
        BufferedImage img = solidImage(TARGET, TARGET, new Color(255, 128, 64));
        float[] chw = ImagePreprocessor.toChwFloat(img);
        assertThat(chw).hasSize(3 * TARGET * TARGET);
    }

    @Test
    void toChwFloat_normalisesValuesToUnitRange() {
        BufferedImage img = solidImage(TARGET, TARGET, new Color(255, 0, 128));
        float[] chw = ImagePreprocessor.toChwFloat(img);

        for (float v : chw) {
            assertThat(v).isBetween(0.0f, 1.0f);
        }
    }

    @Test
    void toChwFloat_correctChannelLayout() {
        // Red=200, Green=100, Blue=50 — verify CHW order
        BufferedImage img = solidImage(4, 4, new Color(200, 100, 50));
        float[] chw = ImagePreprocessor.toChwFloat(img);

        int pixels = 4 * 4;
        float expectedR = 200 / 255.0f;
        float expectedG = 100 / 255.0f;
        float expectedB =  50 / 255.0f;

        // All red-channel pixels are in [0, pixels)
        for (int i = 0; i < pixels; i++) {
            assertThat(chw[i]).isCloseTo(expectedR, offset(1e-5f));
        }
        // All green-channel pixels are in [pixels, 2*pixels)
        for (int i = pixels; i < 2 * pixels; i++) {
            assertThat(chw[i]).isCloseTo(expectedG, offset(1e-5f));
        }
        // All blue-channel pixels are in [2*pixels, 3*pixels)
        for (int i = 2 * pixels; i < 3 * pixels; i++) {
            assertThat(chw[i]).isCloseTo(expectedB, offset(1e-5f));
        }
    }

    // ── letterbox ───────────────────────────────────────────────────────────

    @Test
    void letterbox_squareImageProducesSameSize() {
        BufferedImage src = solidImage(640, 640, Color.RED);
        BufferedImage out = ImagePreprocessor.letterbox(src, TARGET);
        assertThat(out.getWidth()).isEqualTo(TARGET);
        assertThat(out.getHeight()).isEqualTo(TARGET);
    }

    @Test
    void letterbox_wideImageFitsWithinTarget() {
        BufferedImage src = solidImage(1280, 480, Color.BLUE);
        BufferedImage out = ImagePreprocessor.letterbox(src, TARGET);
        assertThat(out.getWidth()).isEqualTo(TARGET);
        assertThat(out.getHeight()).isEqualTo(TARGET);
    }

    @Test
    void letterbox_tallImageFitsWithinTarget() {
        BufferedImage src = solidImage(320, 960, Color.GREEN);
        BufferedImage out = ImagePreprocessor.letterbox(src, TARGET);
        assertThat(out.getWidth()).isEqualTo(TARGET);
        assertThat(out.getHeight()).isEqualTo(TARGET);
    }

    @Test
    void letterbox_paddingAreaIsBlack() {
        // Wide image (2:1 ratio) will have black bars top and bottom
        BufferedImage src = solidImage(640, 320, Color.WHITE);
        BufferedImage out = ImagePreprocessor.letterbox(src, TARGET);

        // Top-left corner pixel should be black (padding)
        int topLeft = out.getRGB(0, 0);
        assertThat(topLeft & 0xFFFFFF).isEqualTo(0x000000);
    }

    // ── preprocess (integration of steps, no ORT tensor allocation) ─────────

    @Test
    void toChwFloat_afterLetterbox_valuesInUnitRange() throws IOException {
        byte[] jpegBytes = solidJpegBytes(800, 600, new Color(128, 64, 200));
        BufferedImage original = ImageIO.read(new java.io.ByteArrayInputStream(jpegBytes));
        BufferedImage letterboxed = ImagePreprocessor.letterbox(original, TARGET);
        float[] chw = ImagePreprocessor.toChwFloat(letterboxed);

        assertThat(chw).hasSize(3 * TARGET * TARGET);
        for (float v : chw) {
            assertThat(v).isBetween(0.0f, 1.0f);
        }
    }

    @Test
    void preprocess_throwsOnUnreadableBytes() {
        assertThatThrownBy(() -> ImagePreprocessor.preprocess(new byte[]{0x00, 0x01, 0x02}, null))
                .isInstanceOf(IOException.class);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private static BufferedImage solidImage(int w, int h, Color color) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    private static byte[] solidJpegBytes(int w, int h, Color color) throws IOException {
        BufferedImage img = solidImage(w, h, color);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpeg", baos);
        return baos.toByteArray();
    }
}
