# Agent Instructions — URL2APK Builder

## APK Delivery Procedure

After the GitHub Actions build completes successfully, the agent MUST deliver the APK to the user through **both** channels below — not just one. This is mandatory; skipping either channel leaves the user without a working download.

### Channel 1: Pull artifact and deliver the file directly

1. **List artifacts** for the completed workflow run:
   ```
   GITHUB_LIST_WORKFLOW_RUN_ARTIFACTS(owner, repo, run_id)
   ```
2. **Download the artifact** (returns a ZIP containing the APK):
   ```
   GITHUB_DOWNLOAD_AN_ARTIFACT(owner, repo, artifact_id)
   ```
3. **Extract the APK** from the downloaded ZIP in the sandbox:
   ```bash
   curl -sL -o app.zip "<s3url from download result>"
   unzip -o app.zip -d /scratch/work/
   ```
4. **Rename** the extracted `app-release-unsigned.apk` to `<AppName>-<version>.apk`.
5. **Promote to workspace output**:
   ```
   workspace_emit(local_path="/scratch/work/<AppName>-<version>.apk", name="<AppName>-<version>.apk")
   ```
6. **Deliver to user**:
   ```
   present_output(artifact_ids=[...], caption="...")
   ```

### Channel 2: Create a GitHub Release for a permanent download link

1. **Ensure the repository is public** so the download link works without authentication:
   ```
   GITHUB_UPDATE_A_REPOSITORY(owner, repo, private=false)
   ```
2. **Create a release** tagged `v<version>`:
   ```
   GITHUB_CREATE_A_RELEASE(owner, repo, tag_name="v1.0.0", target_commitish="main", name="<AppName> v1.0.0")
   ```
3. **Upload the APK as a release asset** (use `upload()` helper first to get s3key):
   ```
   upload(toolkit="github", tool_slug="GITHUB_UPLOAD_RELEASE_ASSET", artifact_id=<from workspace_emit>)
   GITHUB_UPLOAD_RELEASE_ASSET(owner, repo, release_id, asset={name, mimetype, s3key}, content_type="application/vnd.android.package-archive")
   ```
4. **Provide the `browser_download_url`** to the user in the final message. This URL has the format:
   ```
   https://github.com/<owner>/<repo>/releases/download/v<version>/<AppName>-<version>.apk
   ```

### Why both channels

- **Channel 1** gives the user an immediate in-chat download tile — no browser, no redirects.
- **Channel 2** gives a permanent, shareable URL. GitHub release download links redirect (HTTP 302) which can fail in in-app browsers (WhatsApp, Telegram, Instagram). Always provide **both** the direct file and the URL so the user has a fallback.

### Troubleshooting

- If `GITHUB_DOWNLOAD_AN_ARTIFACT` returns an S3 URL, use `curl` to download — the sandbox has public HTTPS access.
- If the APK inside the ZIP is named `app-release-unsigned.apk`, rename it to something user-friendly before delivering.
- If the release download link returns 404, the repository is likely still private — call `GITHUB_UPDATE_A_REPOSITORY` to set `private=false`.
- If the user reports the link doesn't open on mobile, suggest opening in Chrome (not an in-app browser) or provide the release page URL: `https://github.com/<owner>/<repo>/releases/tag/v<version>`.

## Build Pipeline Summary

1. Collect inputs (URL, app name, package ID, version, APK/AAB).
2. Generate app icon in all Android densities (mdpi–xxxhdpi).
3. Generate the Gradle project (TWA or WebView-based depending on features).
4. Push to a GitHub repo via `GITHUB_COMMIT_MULTIPLE_FILES`.
5. Wait for the GitHub Actions workflow to build (triggered on push).
6. Deliver the APK via **both** channels above.
