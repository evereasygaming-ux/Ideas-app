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
- [ ] Phase 1.5: Build pipeline test (CURRENT)
- [ ] Phase 2: Bootstrap (proot + Alpine)
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

## PHASE 1.5 — Build Pipeline Test (CURRENT)

Tasks:
- [ ] 1.5.1: build.yml mein `chmod +x gradlew` step add karo
- [ ] 1.5.2: build.yml mein `--stacktrace` flag add karo
- [ ] 1.5.3: build.yml mein `setup-android` action confirm hata diya
- [ ] 1.5.4: User push kare, build status check kare
- [ ] 1.5.5: Build fail ho toh error log fix karo

## PHASE 2 — Bootstrap System
Files:
- [ ] bootstrap.sh (Alpine extract + Node install)
- [ ] Bootstrap.java (assets se runtime setup)
- [ ] ServerManager.java (PRoot se Node start)
- [ ] MainActivity.java update

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
