# AWS IAM Access Review Tool

A Java-based security auditing tool that helps identify potential security risks in AWS IAM user accounts.

The idea behind this project is simple: reviewing user access manually can take time, especially when an organization has many accounts. This tool makes the process easier by checking account security information, highlighting possible risks, and generating a report that can be reviewed.

It supports both sample CSV files and real AWS IAM credential reports.

## Features

The tool checks for five common access-related security concerns:

- **MFA Disabled:** Identifies users with console access who haven't enabled multi-factor authentication.
- **Inactive Accounts:** Flags accounts with recorded inactivity of 90 days or more.
- **Old Access Keys:** Identifies active access keys that are 90 days old or older.
- **Administrator Access:** Highlights privileged users who may require additional review.
- **Former Employee Access:** Flags accounts belonging to former employees that still have active access.

The last two checks require additional employee and administrator information. They are not automatically verified through AWS credential reports alone.

Other features include:

- Automatic retrieval of AWS IAM credential reports using AWS CLI
- Risk classification (Critical, High, Medium, Low, and Review)
- CSV reports with explanations and recommended actions
- Command-line execution without a database or web server
- Automated tests for audit rules and input handling

## Technologies Used

| Technology | Purpose |
|---|---|
| Java | Main application and security checks |
| AWS IAM | Source of credential and account security information |
| AWS CLI | Secure retrieval of live AWS credential reports |
| CSV | Input files and audit reports |
| Git & GitHub | Version control and project hosting |

## How It Works

The application reads IAM account information, applies security rules, and prepares a report of findings.

```text
AWS IAM / Sample CSV
         |
         v
   Account Data
         |
         v
 Java Audit Engine
         |
         v
  Security Checks
         |
         v
  Risk Classification
         |
         v
 CSV Report + Summary
```

For live AWS mode, the application uses an authenticated AWS CLI profile to retrieve credential reports automatically. It does not modify AWS accounts or permissions.

## Getting Started

### Requirements

- Java 21 or newer
- Terminal or command prompt
- AWS CLI v2 (only for live AWS mode)
- An authorized AWS account with credential-report permissions (only for live mode)

### 1. Clone the Repository

```bash
git clone https://github.com/dhruvvats1/Java-IAM-reviewer.git
cd Java-IAM-reviewer
```

### 2. Build the Project

On macOS or Linux:

```bash
bash scripts/build.sh
```

On Windows:

```bat
scripts\build.bat
```

### 3. Run with Sample Data

```bash
java -jar target/iam-review.jar sample-data/users.csv
```

This runs the audit using fictional accounts included with the project.

### 4. Run with Live AWS Data

First, configure an authorized AWS CLI profile with permission to generate and retrieve IAM credential reports.

Then run:

```bash
java -jar target/iam-review.jar --aws-live --profile iam-review
```

The application retrieves the credential report, analyzes supported IAM security conditions, and saves the findings locally.

The AWS profile should use a restricted identity rather than root credentials.

### 5. Run Automated Tests

On macOS or Linux:

```bash
bash scripts/test.sh
```

On Windows:

```bat
scripts\test.bat
```

## Sample Audit Output

Example output using fictional test data:

```text
=== IAM ACCESS REVIEW ===

Accounts scanned: 8
Security findings: 8

CRITICAL: 1
HIGH:     1
MEDIUM:   5
LOW:      0
REVIEW:   1

Report saved: reports/exceptions.csv
```

Each finding includes the username, security check, severity, explanation, and recommended action.

## Project Structure

```text
src/
  main/java/com/iamreview/
    Main.java                  - Application entry point
    Account.java               - Account data model
    AccountLoader.java         - CSV input handling
    AwsReportLoader.java       - AWS report processing
    AwsCliReportFetcher.java   - Live AWS report retrieval
    AuditService.java          - Security checks
    Finding.java               - Security findings
    ReportWriter.java          - Report generation

  test/java/com/iamreview/
    AuditTests.java            - Automated tests

sample-data/                   - Fictional sample accounts
scripts/                       - Build and test scripts
docs/                          - Additional documentation
```

## Current Limitations

This is a lightweight auditing tool intended for learning, demonstration, and initial security reviews.

- Live AWS integration uses AWS CLI rather than the AWS SDK for Java.
- Administrator access and former employee checks require verified additional data.
- IAM credential reports do not provide a complete view of AWS permissions or activity.
- The tool reports potential security risks but does not automatically change account permissions.
- Root accounts are excluded from normal IAM user checks.

## Future Improvements

- Direct integration with the AWS SDK for Java
- More detailed IAM role and policy analysis
- Configurable security thresholds
- Improved reporting and export options

## What I Learned

Building this project helped me understand how IAM security reviews work and how Java can automate repetitive security checks.

I also gained practical experience working with AWS credential reports, command-line applications, CSV processing, automated testing, and GitHub.

One important lesson was that security findings need context. For example, an administrator account may be legitimate, and missing activity information doesn't automatically mean an account is inactive.

## Author

**GitHub:** [dhruvvats1](https://github.com/dhruvvats1)

---

*This project is intended for educational purposes and authorized security auditing.*
## Limitations / future roadmap

1. **Not a live AWS scanner:** SDK retrieval is the next step; don't claim it is built yet.
2. Admin permissions come from reviewer-provided data. In production, analyze identity policies, group membership, permission boundaries and relevant organizational controls before describing effective admin rights.
3. `last_login` in normalized mode is one provided date. AWS mode combines observed password and access key use but cannot prove there was no activity in other credential types or services.
4. The 90-day policy is an example, not an AWS-mandated rotation policy. Add CLI-configurable thresholds later.
5. Real compliance workflows need an approver, exceptions handling, audit trails and data governance; this is a first-pass triage tool.
6. Security note: real AWS exports can contain account identifiers and sensitive operational metadata. Never upload live exports, credentials, or private employee records to a public repository.

## Demo / interview prep

See `docs/INTERVIEW_GUIDE.md` and `docs/DEADLINE_PLAN.md`.

## Interactive Java + web demo

The optional public dashboard runs the same Java `AuditService` behind an HTTP API and is deliberately **sample-data-only**. It supports synthetic CSV uploads, severity filtering, and CSV export. Live AWS CLI access is only used from the local private CLI; the public web service has no AWS credentials.

Start locally:

```bash
bash scripts/build.sh
java -cp target/iam-review.jar com.iamreview.WebServer
```

Open `http://localhost:8080`. For free Docker-based deployment and demo recording instructions, read [WEB_DEMO_GUIDE.md](WEB_DEMO_GUIDE.md).
