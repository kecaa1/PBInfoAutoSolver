# PBInfo AutoSolver

Android client + FastAPI backend prototype.

## Android app
- Sign in to PBInfo inside the app.
- Enter a numeric problem ID and open its page.
- Tap **Read problem and generate solution**. The app extracts visible page text and sends it to the configured HTTPS backend.
- The app displays the returned C++ solution and explanation. Review and test before submitting manually.

## Backend setup required
The app cannot generate answers until the backend is deployed and its HTTPS `/solve` URL is entered in the app.

Deploy the `backend/` directory as a Python web service:
- Build command: `pip install -r requirements.txt`
- Start command: `uvicorn main:app --host 0.0.0.0 --port $PORT`
- Set environment variable `GEMINI_API_KEY` in the hosting provider's private environment settings.
- Optional: set `GEMINI_MODEL` (defaults to `gemini-2.5-flash`).
- Never put API keys or PBInfo passwords in the Android app or GitHub source.

Use a newly generated, private Gemini key only. The key previously pasted into chat must be revoked and must not be used.

## Current limitations
- The Gemini backend must be deployed separately; it is not hosted by GitHub Actions.
- Generated answers can be wrong. Check the explanation, constraints, and tests.
- No automatic PBInfo submission is performed. Submit only after reviewing the code.
- This prototype does not compile or run hidden tests yet.
- The app reads visible page text; if PBInfo changes its page layout or hides the statement, extraction may need adjustment.

## Build APK
Open Actions, run **Build Android APK**, and download `PBInfoAutoSolver-debug-apk` from the successful run's Artifacts section.
