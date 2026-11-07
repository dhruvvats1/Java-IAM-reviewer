package com.iamreview;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import static com.iamreview.Finding.Severity.*;

public final class AuditService {
    public List<Finding> audit(List<Account> accounts, LocalDate asOf) {
        List<Finding> findings = new ArrayList<>();
        for (Account a : accounts) {
            if (!a.accountActive()) continue; // disabled accounts do not represent active access in this input model
            if (!a.employed()) {
                findings.add(new Finding(a.username(), "FORMER_EMPLOYEE_ACTIVE", CRITICAL,
                    "Former employee still has an active account",
                    "Verify employment status and promptly disable or remove remaining access"));
            }
            if (a.consoleAccess() && !a.mfaEnabled()) {
                findings.add(new Finding(a.username(), "MFA_DISABLED", HIGH,
                    "Active console access without MFA", "Require MFA for interactive sign-in"));
            }
            if (a.lastLogin() != null) {
                long idleDays = ChronoUnit.DAYS.between(a.lastLogin(), asOf);
                if (idleDays < 0) {
                    findings.add(new Finding(a.username(), "FUTURE_LOGIN_DATE", REVIEW,
                        "Last-login date is after the audit date", "Verify source data and dates"));
                } else if (idleDays >= 90) {
                    findings.add(new Finding(a.username(), "INACTIVE_90_DAYS", MEDIUM,
                        "Last recorded login was " + idleDays + " days ago",
                        "Confirm account ownership and disable unused access after approval"));
                }
            } else {
                findings.add(new Finding(a.username(), "UNKNOWN_LOGIN", REVIEW,
                    "No valid last-login date supplied; inactivity cannot be determined",
                    "Check account creation date and other activity logs before classifying inactivity"));
            }
            if (a.accessKeyActive()) {
                if (a.accessKeyAgeDays() == null) {
                    findings.add(new Finding(a.username(), "UNKNOWN_KEY_AGE", REVIEW,
                        "Active key has no age data", "Verify access-key creation or rotation date"));
                } else if (a.accessKeyAgeDays() >= 90) {
                    findings.add(new Finding(a.username(), "OLD_ACCESS_KEY", MEDIUM,
                        "Active access key is " + a.accessKeyAgeDays() + " days old",
                        "Review necessity and rotate or replace key under organization policy"));
                }
            }
            if (a.adminRights()) {
                findings.add(new Finding(a.username(), "PRIVILEGED_ACCESS_REVIEW", MEDIUM,
                    "Account has administrator rights; review required, not an automatic policy violation",
                    "Confirm business need, approval and least-privilege permissions"));
            }
        }
        return findings;
    }
}
