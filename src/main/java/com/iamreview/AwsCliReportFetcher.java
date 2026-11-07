package com.iamreview;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Uses the local, browser-authenticated AWS CLI. Never reads or stores AWS keys. */
public final class AwsCliReportFetcher {
    private final String cli;

    public AwsCliReportFetcher() { this("aws"); }
    AwsCliReportFetcher(String cli) { this.cli = cli; }

    public void fetch(String profile, Path destination) throws Exception {
        if (!profile.matches("[A-Za-z0-9_.-]{1,128}"))
            throw new IllegalArgumentException("Invalid AWS profile name");
        String arn = call(profile, "sts", "get-caller-identity", "--query", "Arn", "--output", "text").trim();
        if (arn.endsWith(":root") || !arn.contains(":user/"))
            throw new IllegalStateException("Refusing AWS live mode: profile must be a dedicated IAM user, not root or another identity");
        boolean ready = false;
        for (int attempt = 0; attempt < 10; attempt++) {
            String state = call(profile, "iam", "generate-credential-report", "--query", "State", "--output", "text").trim();
            if ("COMPLETE".equals(state)) { ready = true; break; }
            if (!"STARTED".equals(state) && !"INPROGRESS".equals(state))
                throw new IOException("AWS credential report state was unexpected: " + state);
            if (attempt < 9) Thread.sleep(1500);
        }
        if (!ready) throw new IOException("Credential report not ready. Run the command again later.");
        String encoded = call(profile, "iam", "get-credential-report", "--query", "Content", "--output", "text").trim();
        byte[] csv;
        try { csv = Base64.getMimeDecoder().decode(encoded); }
        catch (IllegalArgumentException e) { throw new IOException("AWS returned an invalid credential-report encoding"); }
        if (csv.length > 10_000_000 || !new String(csv, StandardCharsets.UTF_8).startsWith("user,arn,"))
            throw new IOException("AWS response was not a valid credential-report CSV");
        Path parent = destination.toAbsolutePath().getParent();
        if (parent != null) Files.createDirectories(parent);
        Files.write(destination, csv);
        try {
            Set<PosixFilePermission> perms = EnumSet.of(PosixFilePermission.OWNER_READ, PosixFilePermission.OWNER_WRITE);
            Files.setPosixFilePermissions(destination, perms);
        } catch (UnsupportedOperationException ignored) { /* Windows */ }
    }

    private String call(String profile, String... args) throws Exception {
        List<String> command = new ArrayList<>();
        command.add(cli);
        command.addAll(List.of(args));
        command.add("--profile"); command.add(profile);
        command.add("--no-cli-pager");
        Process proc = new ProcessBuilder(command).redirectErrorStream(true).start();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (var stream = proc.getInputStream()) {
            byte[] buffer = new byte[8192]; int n;
            while ((n = stream.read(buffer)) != -1) {
                if (output.size() + n > 11_000_000) {
                    proc.destroyForcibly(); throw new IOException("AWS command output exceeded the size limit");
                }
                output.write(buffer, 0, n);
            }
        }
        if (!proc.waitFor(30, TimeUnit.SECONDS)) {
            proc.destroyForcibly(); throw new IOException("AWS CLI request timed out");
        }
        if (proc.exitValue() != 0)
            throw new IOException("AWS CLI request failed. Check profile login and IAM credential-report permissions (exit " + proc.exitValue() + ")");
        return output.toString(StandardCharsets.UTF_8);
    }
}
