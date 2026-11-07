package com.iamreview;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;

/** Public portfolio demo. Intentionally has NO AWS connection or credentials. */
public final class WebServer {
    private static final int MAX_CSV_BYTES = 65536;
    private static final Path DEMO_DIR = Path.of("web-demo");
    private WebServer() {}

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress("0.0.0.0", port), 0);
        server.createContext("/", WebServer::route);
        server.setExecutor(Executors.newFixedThreadPool(4));
        server.start();
        System.out.println("IAM demo running on port " + port + " (synthetic-data mode only)");
    }

    private static void route(HttpExchange ex) throws IOException {
        try {
            ex.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            ex.getResponseHeaders().set("Referrer-Policy", "no-referrer");
            ex.getResponseHeaders().set("Cache-Control", "no-store");
            ex.getResponseHeaders().set("Content-Security-Policy", "default-src 'self'; script-src 'self'; style-src 'self'; img-src 'self' data:; connect-src 'self'; object-src 'none'; frame-ancestors 'none'; base-uri 'none'");
            String url = ex.getRequestURI().getPath();
            String method = ex.getRequestMethod();
            if (url.equals("/health") && method.equals("GET")) {
                respond(ex, 200, "application/json", "{\"status\":\"ok\",\"mode\":\"sample-only\"}");
            } else if (url.equals("/api/audit") && method.equals("POST")) {
                String contentType = ex.getRequestHeaders().getFirst("Content-Type");
                if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("text/csv")) {
                    respond(ex, 415, "application/json", "{\"error\":\"Upload CSV text only. Never upload actual AWS credentials or private reports.\"}");
                    return;
                }
                byte[] bytes = ex.getRequestBody().readNBytes(MAX_CSV_BYTES + 1);
                if (bytes.length > MAX_CSV_BYTES) { respond(ex, 413, "application/json", "{\"error\":\"CSV exceeds 64 KB demo limit\"}"); return; }
                Path temp = Files.createTempFile("iam-demo-", ".csv");
                try {
                    Files.write(temp, bytes);
                    List<Account> accounts = AccountLoader.load(temp);
                    if (accounts.size() > 500) { respond(ex, 413, "application/json", "{\"error\":\"Maximum 500 accounts\"}"); return; }
                    LocalDate date = LocalDate.now();
                    String query = ex.getRequestURI().getRawQuery();
                    if (query != null && query.matches("date=\\d{4}-\\d{2}-\\d{2}")) date = LocalDate.parse(query.substring(5));
                    respond(ex, 200, "application/json", auditJson(accounts, date));
                } finally { Files.deleteIfExists(temp); }
            } else if (url.equals("/api/sample") && method.equals("GET")) {
                // Stable sample date makes examples repeatable, even when deployed in later years.
                respond(ex, 200, "application/json", auditJson(AccountLoader.load(Path.of("sample-data/users.csv")), LocalDate.of(2026,11,7)));
            } else if (method.equals("GET") && (url.equals("/") || url.equals("/index.html") || url.equals("/style.css") || url.equals("/app.js"))) {
                String file = url.equals("/") ? "index.html" : url.substring(1);
                String mime = file.endsWith(".css") ? "text/css" : file.endsWith(".js") ? "application/javascript" : "text/html";
                respond(ex, 200, mime, Files.readString(DEMO_DIR.resolve(file), StandardCharsets.UTF_8));
            } else { respond(ex, 404, "application/json", "{\"error\":\"Not found\"}"); }
        } catch (IllegalArgumentException exn) {
            respond(ex, 400, "application/json", "{\"error\":" + json(exn.getMessage()) + "}");
        } catch (Exception exn) {
            System.err.println("Demo request error: " + exn.getClass().getSimpleName());
            respond(ex, 500, "application/json", "{\"error\":\"Unable to process request\"}");
        } finally { ex.close(); }
    }

    private static String auditJson(List<Account> accounts, LocalDate date) {
        List<Finding> findings = new AuditService().audit(accounts, date);
        StringBuilder out = new StringBuilder("{\"date\":").append(json(date.toString())).append(",\"accounts\":").append(accounts.size()).append(",\"findings\":[");
        for (int i = 0; i < findings.size(); i++) {
            Finding f = findings.get(i);
            if (i > 0) out.append(',');
            out.append("{\"username\":").append(json(f.user())).append(",\"check\":").append(json(f.check()))
                .append(",\"severity\":").append(json(f.severity().name()))
                .append(",\"explanation\":").append(json(f.explanation()))
                .append(",\"recommendation\":").append(json(f.recommendation())).append('}');
        }
        return out.append("]}").toString();
    }

    private static String json(String s) {
        StringBuilder b = new StringBuilder("\"");
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch(c) {
                case '\\' -> b.append("\\\\");
                case '"' -> b.append("\\\"");
                case '\n' -> b.append("\\n");
                case '\r' -> b.append("\\r");
                case '\t' -> b.append("\\t");
                default -> { if (c < 32) b.append(String.format("\\u%04x", (int)c)); else b.append(c); }
            }
        }
        return b.append('"').toString();
    }

    private static void respond(HttpExchange ex, int code, String type, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", type + "; charset=utf-8");
        ex.sendResponseHeaders(code, bytes.length);
        try (var os = ex.getResponseBody()) { os.write(bytes); }
    }
}
