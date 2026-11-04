# Contributing

1. Fork, create a branch, and run `./gradlew test lintDebug assembleDebug` before opening a PR.
2. Detection changes need a test in `parser/src/test/`. Use **redacted** real notification text:
   replace names, phone numbers, account numbers and balances.
3. Every user-facing string goes in **both** `values/strings.xml` and `values-ne/strings.xml`.
   Lint fails the build on a missing translation.
4. UI follows [docs/DESIGN.md](docs/DESIGN.md): semantic colour tokens only, 48 dp touch targets,
   light and dark mode, and screen-reader labels.
5. Never add the `INTERNET` permission or any analytics / ads SDK. PRs that do will be closed.
6. Dependency bumps come through Dependabot; check the build and test on a real device.
