# IDEAS - Project Context

## Goal
Self-contained Android APK: AI-powered coding assistant with own runtime.

## Tech Stack
- Android (Java + WebView)
- PRoot + Alpine Linux (self-contained runtime)
- Node.js (backend server inside Alpine)
- HTML/JS/TailwindCSS (frontend, xterm.js for terminal)
- OpenCode CLI (AI engine)

## Constraints
- Target: Vivo T4x (4GB RAM, Android 15, ARM64)
- APK size target: <50 MB
- No PC available — build via GitHub Actions only
- Bootstrap must be idempotent (safe to re-run)
- Package name: com.ideas.app
- minSdk 24, targetSdk 34

## Folder Structure
- app/src/main/java/com/ideas/app/  → Java code
- app/src/main/assets/              → proot-arm64, alpine-rootfs.tar.gz, www/
- app/src/main/res/                 → layouts, icons
- .github/workflows/                → CI/CD

## Rules
- Har phase ke baad confirm karo
- Ek baar mein ek file banao, phir review ke liye ruko
- Java 17 syntax use karo
- Gradle Kotlin DSL (build.gradle.kts) use karo
