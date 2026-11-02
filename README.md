# PBInfo AutoSolver — Android starter

This repository contains a minimal Android WebView starter app and a GitHub Actions workflow that builds a debug APK.

## Current functionality
- Opens PBInfo inside the app.
- Lets you enter a numeric problem ID and opens that problem page.
- You sign in directly on PBInfo.

## Not implemented yet
This is **not yet an AI solver**. It does not read/solve tasks with an AI model, compile/test C++ submissions, or submit code. Those features require a separately deployed, secured backend and further integration/testing. Do not add PBInfo passwords or AI API keys to this repository.

## Build
Open the Actions tab, run **Build Android APK**, then download the artifact named `PBInfoAutoSolver-debug-apk` from the completed workflow run.
