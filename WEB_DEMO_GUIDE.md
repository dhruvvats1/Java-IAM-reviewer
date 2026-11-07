# Public Java + Web Demo

This is a real Java backend serving a static frontend and a synthetic-data-only audit API. It reuses `AuditService.java` from the original CLI. This **public deployment NEVER authenticates to AWS** and does not have AWS CLI profiles, keys or private data.

## Run locally on Mac

```bash
bash scripts/build.sh
java -cp target/iam-review.jar com.iamreview.WebServer
```

Open http://localhost:8080. Use **Load sample audit** or upload **only synthetic CSVs** with the fields in `sample-data/users.csv`. Browser uploads are processed with Java's `AccountLoader` and `AuditService` and then discarded. CSV size is capped at 64 KB. Press Ctrl+C to stop.

## Deploy to Render free web service

1. Commit the added files: `web-demo/`, `src/main/java/com/iamreview/WebServer.java`, `Dockerfile`, `.dockerignore`, and this guide.
2. Go to https://dashboard.render.com and sign in.
3. **New → Web Service**, connect your GitHub repo `dhruvvats1/Java-IAM-reviewer`.
4. Choose **Docker** as the runtime/environment (the Dockerfile builds Java).
5. Select **Free** if offered; region nearest your audience; no database needed. Leave credentials/environment secrets BLANK.
6. Deploy, wait for `Live`, and open your assigned `https://...onrender.com` URL.
7. Verify Sample Audit, severity filter, Download CSV, and `/health` (must show `sample-only`).

NOTE: Render free services can spin down after idle time and need time to wake on first visit. Check current pricing/limits before selecting a plan. There is no uptime guarantee.

## 60-second screen recording on macOS

1. In Safari/Chrome open the deployed page. Use a browser tab without personal AWS tabs showing.
2. Press `Shift + Command + 5` → **Record Selected Portion**; frame the browser window; click **Record**.
3. Briefly show the dashboard, change severity filter, click **Export CSV** and show the sample report. Optional: show terminal demo locally but NEVER show private AWS details.
4. Click **Stop** in the menu bar; rename video `IAM-Access-Review-Demo.mov`.
5. Upload video to an appropriate shareable platform or attach it to GitHub Releases; link video and deployed URL in README/resume. Verify links in a private/incognito browser.

## Why no live AWS connection on this public website?

AWS IAM credentials and reports should never be exposed through a public unauthenticated API. This portfolio demo intentionally supports synthetic data only. The local Java CLI has a separate `--aws-live` mode using the developer's restricted, local AWS CLI profile.
