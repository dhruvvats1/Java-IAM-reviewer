package com.iamreview;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

/** Dependency-free test harness: executes nonzero on any failed assertion. */
public final class AuditTests {
    private static int tests = 0;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 7);

    public static void main(String[] args) throws Exception {
        check("safe account passes", 0, count(account(true, TODAY.minusDays(1), 30, false, true, true, true, true)));
        check("MFA off flagged", 1, count(account(false, TODAY, 30, false, true, true, true, false)));
        check("MFA off without console access not flagged", 0, count(account(false, TODAY, null, false, true, true, false, false)));
        check("89 days not flagged", 0, count(account(true, TODAY.minusDays(89), null, false, true, true, true, false)));
        check("90 days flagged", 1, count(account(true, TODAY.minusDays(90), null, false, true, true, true, false)));
        check("old login flagged", 1, count(account(true, TODAY.minusDays(120), null, false, true, true, true, false)));
        check("key age 89 not flagged", 0, count(account(true, TODAY, 89, false, true, true, true, true)));
        check("key age 90 flagged", 1, count(account(true, TODAY, 90, false, true, true, true, true)));
        check("inactive key not flagged", 0, count(account(true, TODAY, 120, false, true, true, true, false)));
        check("admin flagged for review", 1, count(account(true, TODAY, null, true, true, true, true, false)));
        check("former employee active critical", 1, count(account(true, TODAY, null, false, false, true, true, false)));
        check("former employee disabled not flagged", 0, count(account(true, TODAY, null, false, false, false, true, false)));
        check("unknown login is review, not inactivity", 1, count(account(true, null, null, false, true, true, true, false)));
        check("future login is review", 1, count(account(true, TODAY.plusDays(2), null, false, true, true, true, false)));
        check("active key missing age is review", 1, count(account(true, TODAY, null, false, true, true, true, true)));
        check("no checks for disabled account", 0, count(account(false, TODAY.minusDays(500), 500, true, false, false, true, true)));
        Finding flagged = new AuditService().audit(List.of(account(true, TODAY, null, false, false, true, true, false)), TODAY).get(0);
        assertCondition("severity critical", flagged.severity() == Finding.Severity.CRITICAL);
        check("quoted CSV parser", 2, Csv.parse("username,role\n\"lee, test\",admin\n").size());
        assertCondition("quoted cell preserved", Csv.parse("a,b\n\"lee, test\",x\n").get(1).get(0).equals("lee, test"));
        assertCondition("escaped quotes roundtrip", Csv.parse(Csv.encode(List.of("a\"b", "x,y"))).get(0).get(0).equals("a\"b"));
        Path temp = Files.createTempFile("iam-review-test", ".csv");
        try {
            Files.writeString(temp, "username,mfa_enabled,last_login,access_key_age_days,admin_rights,employed,account_active,console_access,access_key_active\n" +
                "test,true,2026-11-07,20,false,true,true,true,true\n");
            check("loads good CSV", 1, AccountLoader.load(temp).size());
            Files.writeString(temp, "username,mfa_enabled,last_login,access_key_age_days,admin_rights,employed,account_active,console_access,access_key_active\n" +
                "test,maybe,2026-11-07,20,false,true,true,true,true\n");
            expectFailure("bad Boolean rejected", () -> AccountLoader.load(temp));
            Files.writeString(temp, "username,mfa_enabled,last_login,access_key_age_days,admin_rights,employed,account_active,console_access,access_key_active\n" +
                "test,true,2026-11-07,-4,false,true,true,true,true\n");
            expectFailure("negative key age rejected", () -> AccountLoader.load(temp));
            Files.writeString(temp, "username,mfa_enabled,last_login,access_key_age_days,admin_rights,employed,account_active,console_access,access_key_active\n" +
                "test,true,2026-11-07,10,false,true,true,true,true\ntest,true,2026-11-07,10,false,true,true,true,true\n");
            expectFailure("duplicate username rejected", () -> AccountLoader.load(temp));
            Files.writeString(temp, "username\ntest\n");
            expectFailure("missing header rejected", () -> AccountLoader.load(temp));
            Path report = Files.createTempFile("iam-exceptions", ".csv");
            try {
                ReportWriter.write(report, List.of(new Finding("lee, test", "MFA_DISABLED", Finding.Severity.HIGH, "test", "fix")));
                String output = Files.readString(report);
                assertCondition("CSV report quoting", output.contains("\"lee, test\""));
                check("report header and row", 2, Csv.parse(output).size());
            } finally { Files.deleteIfExists(report); }
        } finally { Files.deleteIfExists(temp); }
        List<Account> awsAccounts = AwsReportLoader.load(Path.of("sample-data/aws-credential-report.csv"),
            Path.of("sample-data/employees.csv"), Path.of("sample-data/admins.csv"), TODAY);
        check("AWS-format sample imported", 3, awsAccounts.size());
        check("AWS-format findings", 6, new AuditService().audit(awsAccounts, TODAY).size());
        assertCondition("former employee found in AWS mode", new AuditService().audit(awsAccounts, TODAY).stream()
            .anyMatch(f -> f.user().equals("aws-charlie") && f.severity() == Finding.Severity.CRITICAL));
        System.out.println("PASS: " + tests + " automated assertions");
    }

    private static Account account(boolean mfa, LocalDate login, Integer age, boolean admin,
                                   boolean employed, boolean active, boolean console, boolean keyActive) {
        return new Account("test-user", mfa, login, age, admin, employed, active, console, keyActive);
    }
    private static int count(Account a) { return new AuditService().audit(List.of(a), TODAY).size(); }
    private static void check(String desc, int expected, int actual) {
        assertCondition(desc + " (expected " + expected + ", got " + actual + ")", expected == actual);
    }
    private static void assertCondition(String desc, boolean ok) {
        tests++; if (!ok) throw new AssertionError("FAIL: " + desc);
    }
    private interface Throwing { void run() throws Exception; }
    private static void expectFailure(String desc, Throwing f) throws Exception {
        boolean rejected = false;
        try { f.run(); } catch (IllegalArgumentException e) { rejected = true; }
        assertCondition(desc, rejected);
    }
}
