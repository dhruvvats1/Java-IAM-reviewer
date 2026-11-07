package com.iamreview;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import static com.iamreview.Finding.Severity;

public final class Main {
    private Main() {}
    public static void main(String[] args) {
        try { run(args); }
        catch (Exception e) { System.err.println("ERROR: " + e.getMessage()); System.exit(1); }
    }
    private static void run(String[] args) throws Exception {
        if (args.length == 0 || args[0].equals("--help")) {
            System.out.println("Usage: java -jar iam-review.jar <accounts.csv> [--out report.csv] [--date YYYY-MM-DD]");
            System.out.println("   or: java -jar iam-review.jar --aws-report <credential-report.csv> --employees <roster.csv> --admins <admins.csv> [--date YYYY-MM-DD] [--out report.csv]");
            System.out.println("   or: java -jar iam-review.jar --aws-live --profile iam-review [--employees roster.csv --admins admins.csv] [--out private-data/live-findings.csv]");
            return;
        }
        boolean live = args[0].equals("--aws-live");
        boolean aws = live || args[0].equals("--aws-report");
        String profile = "iam-review";
        Path input = aws ? null : Path.of(args[0]);
        Path employeeFile = null, adminFile = null;
        Path out = live ? Path.of("private-data", "live-findings.csv") : Path.of("reports", "exceptions.csv");
        LocalDate date = LocalDate.now();
        for (int i = aws ? 0 : 1; i < args.length; i++) {
            if (args[i].equals("--aws-live")) { /* mode selector */ }
            else if (args[i].equals("--aws-report") && i + 1 < args.length) input = Path.of(args[++i]);
            else if (args[i].equals("--profile") && i + 1 < args.length) profile = args[++i];
            else if (args[i].equals("--employees") && i + 1 < args.length) employeeFile = Path.of(args[++i]);
            else if (args[i].equals("--admins") && i + 1 < args.length) adminFile = Path.of(args[++i]);
            else if (args[i].equals("--out") && i + 1 < args.length) out = Path.of(args[++i]);
            else if (args[i].equals("--date") && i + 1 < args.length) {
                try { date = LocalDate.parse(args[++i]); }
                catch (DateTimeParseException e) { throw new IllegalArgumentException("--date must be YYYY-MM-DD"); }
            } else throw new IllegalArgumentException("Unknown or incomplete argument: " + args[i]);
        }
        if (live) {
            input = Path.of("private-data", "aws-live-credential-report.csv");
            new AwsCliReportFetcher().fetch(profile, input);
            System.out.println("AWS credential report retrieved securely using profile: " + profile);
        }
        if (input == null) throw new IllegalArgumentException("Missing input CSV file");
        if (aws && ((employeeFile == null) != (adminFile == null)))
            throw new IllegalArgumentException("Provide both --employees and --admins, or neither for root-only reports");
        List<Account> accounts = aws ? AwsReportLoader.load(input, employeeFile, adminFile, date) : AccountLoader.load(input);
        List<Finding> findings = new AuditService().audit(accounts, date);
        ReportWriter.write(out, findings);
        Map<Severity, Integer> counts = new EnumMap<>(Severity.class);
        for (Severity s : Severity.values()) counts.put(s, 0);
        for (Finding f : findings) counts.put(f.severity(), counts.get(f.severity()) + 1);
        System.out.println("=== IAM ACCESS REVIEW ===");
        System.out.println("Audit date:       " + date);
        System.out.println("Accounts scanned: " + accounts.size());
        System.out.println("Findings:         " + findings.size());
        for (Severity s : Severity.values()) System.out.printf("%-17s %d%n", s + ":", counts.get(s));
        System.out.println("Report saved:     " + out.toAbsolutePath());
        System.out.println("Mode:             " + (live ? "LIVE AWS retrieval via AWS CLI" : aws ? "AWS credential-report CSV import" : "Sample/normalized CSV"));
        if (aws && accounts.isEmpty()) System.out.println("No IAM users found. Root-account records are excluded from user checks.");
        if (aws && employeeFile == null) System.out.println("Scope: MFA/activity/key-age only. Employment and admin rights NOT ASSESSED (no verified rosters).");
        System.out.println("Note: IAM credential metadata analysis, not a full AWS authorization verdict.");
    }
}
