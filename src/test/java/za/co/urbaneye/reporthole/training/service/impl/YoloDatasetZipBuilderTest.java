package za.co.urbaneye.reporthole.training.service.impl;

import org.junit.jupiter.api.Test;
import za.co.urbaneye.reporthole.incident.entity.IssueType;
import za.co.urbaneye.reporthole.training.entity.IssueAnnotation;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class YoloDatasetZipBuilderTest {

    private static IssueAnnotation box(IssueType type, double x, double y, double w, double h) {
        return IssueAnnotation.builder().classLabel(type).xCenter(x).yCenter(y).width(w).height(h).build();
    }

    @Test
    void everyIssueTypeHasAYoloClassIndex() {
        assertThat(YoloDatasetZipBuilder.CLASS_ORDER).containsExactlyInAnyOrder(IssueType.values());
    }

    @Test
    void labelFile_usesLocaleIndependentSixDecimalLines() {
        String label = YoloDatasetZipBuilder.labelFile(List.of(
                box(IssueType.POTHOLE, 0.5, 0.25, 0.1, 0.2),
                box(IssueType.CRACK, 0.123456789, 0.5, 0.3, 0.4)));

        assertThat(label.lines().toList()).containsExactly(
                "0 0.500000 0.250000 0.100000 0.200000",
                "1 0.123457 0.500000 0.300000 0.400000");
    }

    @Test
    void build_writesImagesLabelsAndDataYaml() throws Exception {
        byte[] zip = YoloDatasetZipBuilder.build(List.of(new YoloDatasetZipBuilder.Sample(
                "abc", new byte[]{9, 8, 7}, List.of(box(IssueType.POTHOLE, 0.5, 0.5, 0.2, 0.2)))));

        Map<String, byte[]> entries = new HashMap<>();
        try (ZipInputStream in = new ZipInputStream(new ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = in.getNextEntry()) != null) {
                entries.put(e.getName(), in.readAllBytes());
            }
        }

        assertThat(entries).containsOnlyKeys("images/abc.jpg", "labels/abc.txt", "data.yaml");
        assertThat(entries.get("images/abc.jpg")).containsExactly(9, 8, 7);
        assertThat(new String(entries.get("labels/abc.txt"), StandardCharsets.UTF_8))
                .isEqualTo("0 0.500000 0.500000 0.200000 0.200000" + System.lineSeparator());
        assertThat(new String(entries.get("data.yaml"), StandardCharsets.UTF_8))
                .contains("nc: 8").contains("  0: POTHOLE").contains("  1: CRACK");
    }
}
