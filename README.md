# IAM Access Review & Security Audit Automation Tool

**Java 21 | Command-line interface | CSV auditing | live AWS CLI report retrieval | automated tests**

A dependency-free, local Java application that performs a first-pass review of IAM-style user accounts. The tool identifies risky or review-worthy accounts and exports a CSV report with a user, check, severity, explanation and recommended action. In `--aws-live` mode, Java invokes the authenticated AWS CLI to retrieve the IAM credential report automatically. It **does not** modify IAM users, policies, or access keys. This version uses the AWS CLI bridge, not the AWS SDK for Java.

## What it checks

| Check | When reported | Priority |
|---|---|---|
| `MFA_DISABLED` | Active console-enabled user without MFA | HIGH |
| `INACTIVE_90_DAYS` | Active account's latest known login/activity date is at least 90 days old | MEDIUM |
| `OLD_ACCESS_KEY` | Any active key has an age of 90 days or more | MEDIUM |
| `PRIVILEGED_ACCESS_REVIEW` | Account explicitly flagged as admin in input/roster | MEDIUM (review, not automatically insecure) |
| `FORMER_EMPLOYEE_ACTIVE` | Employee roster says departed and account has active credentials | CRITICAL |
| `UNKNOWN_LOGIN`, `UNKNOWN_KEY_AGE`, `FUTURE_LOGIN_DATE` | Missing or inconsistent activity data | REVIEW |

These are **configurable organizational review rules in concept**, not definitive AWS security compliance findings. Threshold is hard-coded to 90 days in the current implementation.

## Requirements

- Java Development Kit (JDK) **21** (`java`, `javac`, and `jar` on PATH).
- No Maven, database, web server, or extra Java libraries required.
- For `--aws-live`, AWS CLI v2 installed, an authorized non-root IAM user signed in via `aws login`, and permissions `iam:GenerateCredentialReport` and `iam:GetCredentialReport`.

## Quick start — Windows (PowerShell / Command Prompt)

From the extracted project's root folder:

```bat
scripts\build.bat
java -jar target\iam-review.jar sample-data\users.csv --date 2026-11-07
scripts\test.bat
```

Run without `--date` to use the computer's current date. Default output is `reports/exceptions.csv`. Example:

```bat
java -jar target\iam-review.jar sample-data\users.csv --out reports\my-report.csv
```

## Quick start — macOS / Linux

```bash
./scripts/build.sh
java -jar target/iam-review.jar sample-data/users.csv --date 2026-11-07
./scripts/test.sh
```

The above sample is synthetic; `--date 2026-11-07` keeps the sample test results reproducible. The expected demo output is 8 accounts scanned and 8 findings (1 critical, 1 high, 5 medium, 1 review).

## Live AWS credential-report retrieval — macOS

Before running, sign in **as the dedicated, restricted IAM user**, not root:

```bash
aws sts get-caller-identity --profile iam-review --query Arn --output text
```

The ARN should end in `user/iam-audit-reader`. If login is expired, run `aws login --profile iam-review --region us-east-1` and check the ARN again.

Then from the project root:

```bash
bash scripts/build.sh
java -jar target/iam-review.jar --aws-live --profile iam-review
```

The Java application calls AWS CLI to verify that a **non-root IAM user** is authenticated, generate the credential report if needed, retrieve its base64-encoded contents, and save the private CSV to `private-data/aws-live-credential-report.csv`. It then runs the existing Java audit and writes `private-data/live-findings.csv`. Files in `private-data/` are excluded from Git. No access keys are created, printed, or embedded in source code.

**If the account has only a root record:** output says `Accounts scanned: 0`, with a clear explanation that root isn't included in IAM user checks. This is not a clean security audit verdict. Use the included synthetic data to demonstrate the five user checks.

**If the AWS report contains IAM users:** by default only the checks backed by AWS report fields run (MFA, activity, key age). The employee-status and admin-rights checks are **NOT ASSESSED** unless both independently verified rosters are supplied. For a complete five-check audit, use:

```bash
java -jar target/iam-review.jar --aws-live --profile iam-review \
  --employees private-data/verified-employees.csv \
  --admins private-data/verified-admins.csv
```

Never use sample-data rosters with real account usernames or publish real credential reports. AWS IAM credential reports do not contain employee status or effective administrator access. Avoid treating a low number of findings as proof that an AWS account is secure.

For the deadline-oriented project, live retrieval uses the **AWS CLI from Java**, not `software.amazon.awssdk.services.iam.IamClient`. A future version could use the AWS SDK with Maven dependencies. The CLI is only invoked with fixed subcommands and the validated profile name, with process timeouts and no shell evaluation.

## AWS credential-report CSV mode (offline)

The AWS IAM console lets authorized users export a *credential report* CSV. This project can analyze an AWS-formatted CSV file locally without making API calls. For demonstration, use **only synthetic data**:

```bat
java -jar target\iam-review.jar --aws-report sample-data\aws-credential-report.csv --employees sample-data\employees.csv --admins sample-data\admins.csv --date 2026-11-07 --out reports\aws-exceptions.csv
```

This demo scans 3 synthetic IAM users and produces 6 findings.

- `--employees`: CSV with `username,employed`. You need this because AWS does not know employee exit status.
- `--admins`: CSV with `username,admin_rights`. The credential report does **not** disclose effective administrator rights; this is an **externally verified** synthetic roster for the demo.
- When roster files are supplied, a matching entry is required for every IAM user in the report (excluding `<root_account>`). When rosters are omitted, employment and admin-rights findings are not assessed; no claim of compliance should be made.
- Both AWS access keys are examined, and the oldest active key is used for the key-age rule. Login activity uses the most recently observed console sign-in or active access-key use. No activity information is reported as REVIEW instead of assumed to be an inactivity violation.
- Imported AWS reports do **not** establish effective permissions, group memberships, access through roles, service-specific credentials, or activity invisible to the report.

**Important:** AWS SDK calls / real AWS account integration are *not implemented* in this version. It ingests an AWS-format file, not a live AWS account. For real integrations, plan read-only report retrieval using AWS SDK for Java 2.x and narrowly scoped `iam:GenerateCredentialReport` / `iam:GetCredentialReport` permissions; verify actual access and costs separately.

## Normalized CSV schema

Every header is required (case-insensitive). Valid booleans are `true` or `false`.

| Field | Example | Notes |
|---|---|---|
| `username` | `alice` | Unique account name |
| `mfa_enabled` | `true` | MFA status |
| `last_login` | `2026-11-05` | ISO date or `NEVER` / blank (unknown) |
| `access_key_age_days` | `95` | Age of oldest active key; `N/A` if unknown |
| `admin_rights` | `false` | Privileged review indicator provided by reviewer |
| `employed` | `true` | Employee status supplied by organization |
| `account_active` | `true` | Whether the account is enabled for review purposes |
| `console_access` | `true` | Whether console login exists |
| `access_key_active` | `true` | Whether a programmatic key is enabled |

If account is disabled (`account_active=false`), active-account checks are skipped. The system deliberately does not mark an unknown login date as an inactivity violation, and a key is flagged for age only when active.

## Example output

```text
=== IAM ACCESS REVIEW ===
Audit date:       2026-11-07
Accounts scanned: 8
Findings:         8
CRITICAL:         1
HIGH:             1
MEDIUM:           5
LOW:              0
REVIEW:           1
Report saved:     .../reports/exceptions.csv
Mode:             Sample/normalized CSV
Note: Offline CSV assessment, not a live AWS authorization verdict.
```

CSV report fields: `username,check,severity,explanation,recommendation`.

## Automated tests

No third-party dependencies: an executable Java test suite runs assertions, checks expected failure conditions and exits with a nonzero code on failure.

```bat
scripts\test.bat
```

Or on Unix-like systems: `./scripts/test.sh`. Covers the pass and fail paths of all five checks, 89/90-day boundaries, active versus disabled accounts, unknown dates, malformed CSV values, duplicate users, CSV quoting and report output. See `src/test/java/com/iamreview/AuditTests.java`.

## Structure

```text
src/main/java/com/iamreview/
  Main.java             # CLI arguments, output summary
  Account.java          # Input model
  AccountLoader.java    # Normalized CSV input and validation
  AwsReportLoader.java  # Offline AWS credential report adapter
  AuditService.java     # Risk-detection rules
  Finding.java          # Finding model and severity enum
  Csv.java              # CSV reader / writer helpers
  ReportWriter.java     # CSV exception report
src/test/java/com/iamreview/AuditTests.java
sample-data/             # Synthetic CSV fixtures
scripts/                 # build and test scripts, Windows / macOS / Linux
reports/                 # Generated example reports
```

## Architecture

```text
normalized CSV ------\
                      --> Account model --> AuditService --> Findings --> reports/exceptions.csv
AWS credential CSV --/       ^                                  |
      + employees roster ----|                                  +--> terminal summary
      + admin roster --------|
```

