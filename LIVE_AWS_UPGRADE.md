# Live AWS integration upgrade

This update adds `--aws-live` using your existing AWS CLI browser login. It is **Java invoking AWS CLI**, not the AWS SDK for Java. That keeps the project dependency-free and easy to build on your Mac.

## How to apply the update to your existing VS Code project on macOS

1. Download `IAM_Java_Live_Upgrade.zip` into Downloads.
2. Open VS Code terminal inside the root of your existing `iam-access-review-tool` project.
3. Run:

   ```bash
   unzip -o ~/Downloads/IAM_Java_Live_Upgrade.zip -d .
   bash scripts/build.sh
   bash scripts/test.sh
   ```

4. Confirm 34 automated assertions pass.
5. Verify the CLI session is for your dedicated IAM user rather than root:

   ```bash
   aws sts get-caller-identity --profile iam-review --query Arn --output text
   ```

6. Then run your Java program against AWS:

   ```bash
   java -jar target/iam-review.jar --aws-live --profile iam-review
   ```

7. Find your private live report in `private-data/live-findings.csv` and the retrieved private credential report in `private-data/aws-live-credential-report.csv`.

If the CLI session expired, first run `aws login --profile iam-review --region us-east-1`, then verify the identity again.

### Important
- Do NOT run this as root. The tool refuses root credentials.
- If no verified employee/admin rosters are supplied, their checks are explicitly NOT ASSESSED; the AWS-backed checks still work.
- The `private-data/` folder is gitignored; never publish real reports.
- AWS IAM credential reports show IAM users plus a root record but not all possible identities or effective permissions.
- Do not run `git add -f` on any file containing sensitive account information.

## Upload the update to GitHub

Once working:

```bash
git status
git add .gitignore README.md LIVE_AWS_UPGRADE.md src/main/java/com/iamreview src/test/java/com/iamreview/AuditTests.java
git diff --cached --stat
git commit -m "Add secure live AWS credential report retrieval"
git push origin main
```

Verify `git status` never stages any `private-data/` content before committing.
