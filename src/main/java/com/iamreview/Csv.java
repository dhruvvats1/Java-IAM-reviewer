package com.iamreview;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Small RFC4180-style CSV parser/writer, with quoted commas, quotes and newlines. */
public final class Csv {
    private Csv() {}

    public static List<List<String>> read(Path path) throws IOException {
        String source = Files.readString(path, StandardCharsets.UTF_8);
        if (source.startsWith("\uFEFF")) source = source.substring(1);
        return parse(source);
    }

    public static List<List<String>> parse(String source) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder value = new StringBuilder();
        boolean quoted = false, closedQuote = false, atStart = true;
        for (int i = 0; i < source.length(); i++) {
            char c = source.charAt(i);
            if (quoted) {
                if (c == '"') {
                    if (i + 1 < source.length() && source.charAt(i + 1) == '"') { value.append('"'); i++; }
                    else { quoted = false; closedQuote = true; }
                } else { value.append(c); }
            } else if (c == ',') {
                row.add(value.toString()); value.setLength(0); closedQuote = false; atStart = true;
            } else if (c == '\n' || c == '\r') {
                if (c == '\r' && i + 1 < source.length() && source.charAt(i + 1) == '\n') i++;
                row.add(value.toString()); value.setLength(0);
                if (!(row.size() == 1 && row.get(0).isBlank())) rows.add(row);
                row = new ArrayList<>(); closedQuote = false; atStart = true;
            } else if (c == '"' && atStart && !closedQuote) { quoted = true; atStart = false;
            } else if (closedQuote) { throw new IllegalArgumentException("Unexpected character after closing CSV quote at character " + (i + 1));
            } else if (c == '"') { throw new IllegalArgumentException("Unexpected CSV quote at character " + (i + 1));
            } else { value.append(c); atStart = false; }
        }
        if (quoted) throw new IllegalArgumentException("Unclosed CSV quoted field");
        if (value.length() > 0 || !row.isEmpty() || closedQuote) {
            row.add(value.toString());
            if (!(row.size() == 1 && row.get(0).isBlank())) rows.add(row);
        }
        return rows;
    }

    public static String encode(List<String> fields) {
        List<String> escaped = new ArrayList<>();
        for (String s : fields) {
            if (s == null) s = "";
            if (s.contains(",") || s.contains("\"") || s.contains("\r") || s.contains("\n"))
                escaped.add("\"" + s.replace("\"", "\"\"") + "\"");
            else escaped.add(s);
        }
        return String.join(",", escaped);
    }
}
