# IDEAS App — Master Plan

## Project Info
- App: IDEAS (Integrated Development Environment Assistant Symbiotic)
- Package: com.ideas.app
- Repo: https://github.com/evereasygaming-ux/Ideas-app
- Local: ~/ideas-app
- Device: Vivo T4x (4GB RAM, Android 15, ARM64)
- Build: GitHub Actions (no PC)

## Status
- [x] Phase 1: Android skeleton
- [x] Phase 1.5: Build pipeline test
- [ ] Phase 2: Bootstrap (proot + Alpine) (CURRENT)
- [ ] Phase 3: IDEAS Node.js server
- [ ] Phase 4: WebView UI
- [ ] Phase 5: OpenCode integration
- [ ] Phase 6: Polish

## Rules for OpenCode
1. Ek baar mein EK task karo, phir RUKO
2. Har file banane ke baad `cat` se verify karo
3. Git commands khud mat chalao — user chalayega
4. Bade binaries download mat karo
5. `ls --color=never` use karo (hang se bachne ke liye)

## PHASE 1.5 — Build Pipeline Test

Tasks:
- [x] 1.5.1: build.yml mein `chmod +x gradlew` step add karo
- [x] 1.5.2: build.yml mein `--stacktrace` flag add karo
- [x] 1.5.3: build.yml mein `setup-android` action confirm hata diya
- [x] 1.5.4: User push kare, build status check kare
- [x] 1.5.5: Build fail ho toh error log fix karo

## PHASE 2 — Bootstrap System (CURRENT)
Tasks:
- [x] 2.0: Audit assets — rootfs OK (Alpine 3.20.0 aarch64); proot-arm64 was 0 bytes (blocker)
- [x] 2.0b: Real PRoot ARM64 binary added (jniLibs/arm64-v8a/libproot.so + assets fallback)
- [ ] 2.1: Bootstrap.java (rootfs extract + Node install + verify)
- [ ] 2.2: ServerManager.java (Node a PRoot se start, logs)
- [ ] 2.3: MainActivity.java (progress + ready + retry + errors)
- [ ] 2.4: GitHub Actions build verification (Phase 2)

## PHASE 3 — Node.js Server
Files:
- [ ] server/package.json
- [ ] server/server.js (Express + WebSocket)
- [ ] server/routes/chat.js
- [ ] server/routes/terminal.js

## PHASE 4 — WebView UI
Files:
- [ ] www/index.html
- [ ] www/app.js
- [ ] www/style.css
- [ ] www/manifest.json

## PHASE 5 — OpenCode Integration
- [ ] Chat endpoint OpenCode se connect
- [ ] Session management
- [ ] Streaming responses

## PHASE 6 — Polish
- [ ] App icon
- [ ] Splash screen
- [ ] Error handling
- [ ] Settings screen

## OpenCode Instructions
Jab user bole "PLAN.md padho aur next task karo":
1. PLAN.md kholo
2. Current phase (jo [ ] hai) dhundo
3. Us phase ka PEHLA unchecked task karo
4. Sirf ek task, phir RUKO
5. User ko batao kya kiya, next kya hai
