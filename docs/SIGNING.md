# RoutineNest release signing

On 14 October 2026 the publisher requested a dedicated RoutineNest release key.
It was generated locally, outside the repository:

- Keystore: `C:/Users/User/.codex/signing/routinenest/routinenest-release.jks`
- Alias: `routinenest-release`
- Password: `C:/Users/User/.codex/signing/routinenest/password.dpapi`
- Certificate: 4096-bit RSA, SHA384withRSA, expires 1 March 2054
- Certificate SHA-256: `C9438D60C3F444DA9D7D8765B0635925AE89F08C6EC419ED64A91D4D0F801F3C`

The password file uses Windows DPAPI under the creating Windows account. It is
not a portable recovery copy. Before publishing, arrange a secure backup of the
keystore and a separately recoverable password, such as in your password manager.
Keep both out of Git and retain the key for future signing and local testing.

After building the unsigned release APK, use PowerShell:

```powershell
./scripts/sign-release.ps1
```

The script reads the protected password, signs with the dedicated key, verifies
the APK signature and ZIP alignment, and prints the public certificate/hash.
It does not configure Gradle signing or upload private signing material.
The signed output is `app/build/outputs/apk/release/app-release.apk`.

The Amazon SDK authentication PEM in `app/src/main/assets/` is a public key for
Amazon SDK authentication. It is distinct from this private APK signing key and
the server-only Amazon RVS shared secret. Signing a build does not make its billing
integration tested or authorize store submission.

A verified local backup of the encrypted keystore and its DPAPI password file was created on 14 October 2026, outside Git. See `signing-backup-evidence.json` for its path. This protects against accidental file changes; it remains on this machine and is not a portable/off-machine recovery copy. The publisher instructed submission of the current free release, which is now submitted.
