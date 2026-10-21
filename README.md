## Phone-only cloud build (GitHub Actions)

This project includes `.github/workflows/build-apk.yml`. After the source files are in a GitHub repository:
1. Open the repository's **Actions** tab.
2. Select **Build Android APK** and tap **Run workflow** (or push/commit to `main`).
3. Open the completed run.
4. Under **Artifacts**, download `DualModeLock-debug-apk`.
5. Extract the artifact ZIP and tap `app-debug.apk` to install. Android may ask you to allow installs from the browser/file manager you used.

Keep the repository private if you don't want the source visible publicly. Never put passwords, signing keys, or personal data into source files.

Note: GitHub's mobile website can make uploading nested project folders awkward. The workflow is ready, but you still need to put the project files into a GitHub repository before it can run.
