package com.iamreview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class ReportWriter {
    private ReportWriter() {}
    public static void write(Path output, List<Finding> findings) throws IOException {
        List<String> lines = new ArrayList<>();
        lines.add("username,check,severity,explanation,recommendation");
        for (Finding f : findings)
            lines.add(Csv.encode(List.of(f.user(), f.check(), f.severity().name(), f.explanation(), f.recommendation())));
        Path parent = output.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(output, lines, StandardCharsets.UTF_8);
    }
}
