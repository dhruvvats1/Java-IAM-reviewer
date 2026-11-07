# Interview Guide — IAM Access Review Tool

## 45-second project pitch

"I built a Java command-line IAM review tool to automate a first-pass security audit. It reads a user-account CSV or an exported AWS IAM credential-report CSV, validates the input, and flags missing MFA, inactivity of at least 90 days, old active access keys, admin access needing review, and former employees who still have active credentials. It assigns severity and remediation guidance and writes a CSV exception report. I wrote automated tests for pass/fail cases, boundary dates and incorrect input. The current build is completely offline; live AWS API retrieval would be my next enhancement."

## 3-minute live demo

1. Show `sample-data/users.csv` (fictional names, not real user data).
2. Run `scripts/build.bat` (on Windows) or `./scripts/build.sh`.
3. Run `java -jar target/iam-review.jar sample-data/users.csv --date 2026-11-07` (adjust `/` for Windows if needed).
4. Show terminal summary: 8 accounts scanned, 8 findings, 1 critical.
5. Open `reports/exceptions.csv`; highlight `charlie` (former employee with active access) and `bob` (multiple review issues).
6. Run `scripts/test.bat` or `./scripts/test.sh`; highlight the passing test count.
7. Show AWS-format mode and explain employee/admin rosters are needed because the credential report does not contain them.

## Common interview questions

**Q: What is IAM?**
A: Identity and Access Management controls who can access a cloud resource and what they're allowed to do.

**Q: Why did you choose Java?**
A: Java has strong OOP modeling, date APIs, standard file I/O, and structured testing. A CLI avoids unnecessary infrastructure.

**Q: What does MFA do?**
A: MFA requires an additional authentication factor, reducing the chance that a stolen password alone grants access.

**Q: Why 90 days?**
A: It's the sample organization's review threshold for this project, not a universal AWS mandate.

**Q: Why are admins flagged?**
A: Admin permissions are powerful and need periodic least-privilege review; admin access is not automatically a violation.

**Q: How did you avoid false positives?**
A: I check whether the account or key is active, treat missing activity as REVIEW rather than confirmed inactivity, and use inclusive 90-day date boundaries.

**Q: What's the most important finding?**
A: Former employees with active access; this is critical because their account credentials may still be usable.

**Q: What is the difference between `access_key_age_days` and `last_login`?**
A: One measures how long credentials have existed; the other measures account activity. A long-lived key can be actively used, and a fresh key can belong to an inactive user.

**Q: Does your tool connect to AWS?**
A: Not currently. It processes an offline AWS IAM credential-report export. Live SDK connectivity is planned but not implemented in the submitted version.

**Q: Why do you need employee and admin rosters?**
A: Credential reports don't include HR employment status or effective admin authorization. Those facts require external inputs or additional authorized AWS policy analysis.

**Q: What would you improve next?**
A: Retrieve credential reports with the AWS SDK, map managed/inline/group policies to identify privileged users, allow configurable policies, add structured error logging, and introduce safe handling for account-data exports.

**Q: Which OOP concepts did you use?**
A: Encapsulation through records for data models; separation of concerns in `AccountLoader`, `AuditService` and `ReportWriter`; enums for typed severity; focused methods for testability.

**Q: What's your testing strategy?**
A: Run the security engine on purpose-built synthetic accounts that should pass and fail. Validate 89/90-day edges, disabled access, unknown values, CSV errors and generated output.

## Don't claim

- "It connects to AWS in real time" — false for this version.
- "It detects all administrator permissions" — it uses admin flags/rosters supplied as inputs.
- "It is certified compliant with AWS best practices" — it is a prototype with example policies.
- "I secured a live company" — only synthetic samples were used.

## Resume bullets (only after personally running/understanding the project)

**IAM Access Review & Security Audit Automation Tool — Java 21, CSV, AWS IAM Credential Reports**
- Built a Java CLI that audits user-account CSV exports for five IAM access-review scenarios and generates severity-ranked remediation reports.
- Added offline AWS IAM credential-report CSV ingestion, with separate employee-status and privileged-access inputs.
- Implemented automated Java tests for security-rule pass/fail paths, 90-day boundaries, malformed input and CSV output.
