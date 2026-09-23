package za.co.urbaneye.reporthole.training.service.impl;

import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Lays annotated images out in the Ultralytics/Roboflow YOLO folder structure:
 * {@code images/<name>.jpg}, {@code labels/<name>.txt} (one {@code class x y w h} line per box)
 * and a {@code data.yaml} naming the classes.
 */
final class YoloDatasetZipBuilder {

    /**
     * Class index = position in this list. Deliberately explicit rather than {@code IssueType.ordinal()}
     * so that reordering or inserting an enum constant can never silently re-number exported labels;
     * append new classes at the end.
     */
    static final List<IssueType> CLASS_ORDER = List.of(
            IssueType.POTHOLE,
            IssueType.CRACK,
            IssueType.FADED_MARKINGS,
            IssueType.DAMAGED_SIGN,
            IssueType.BLOCKED_DRAIN,
            IssueType.BROKEN_TRAFFIC_LIGHT,
            IssueType.ACCIDENT,
            IssueType.OTHER);

    /** One image and its boxes; {@code name} is the shared file stem (no extension). */
    record Sample(String name, byte[] imageBytes, List<IssueAnnotation> annotations) {}

    private YoloDatasetZipBuilder() {}

    static byte[] build(List<Sample> samples) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            for (Sample s : samples) {
                write(zip, "images/" + s.name() + ".jpg", s.imageBytes());
                write(zip, "labels/" + s.name() + ".txt", labelFile(s.annotations()).getBytes(StandardCharsets.UTF_8));
            }
            write(zip, "data.yaml", dataYaml().getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    static String labelFile(List<IssueAnnotation> annotations) {
        StringBuilder sb = new StringBuilder();
        for (IssueAnnotation a : annotations) {
            sb.append(String.format(Locale.ROOT, "%d %.6f %.6f %.6f %.6f%n",
                    CLASS_ORDER.indexOf(a.getClassLabel()),
                    a.getXCenter(), a.getYCenter(), a.getWidth(), a.getHeight()));
        }
        return sb.toString();
    }

    static String dataYaml() {
        StringBuilder sb = new StringBuilder("train: images\nval: images\nnc: ")
                .append(CLASS_ORDER.size()).append("\nnames:\n");
        for (int i = 0; i < CLASS_ORDER.size(); i++) {
            sb.append("  ").append(i).append(": ").append(CLASS_ORDER.get(i).name()).append('\n');
        }
        return sb.toString();
    }

    private static void write(ZipOutputStream zip, String path, byte[] bytes) throws IOException {
        zip.putNextEntry(new ZipEntry(path));
        zip.write(bytes);
        zip.closeEntry();
    }
}
