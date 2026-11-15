# Zevra

En Android-app (Kotlin & Jetpack Compose) för kostspårning, AI-måltidsanalys, träning/Health Connect, receptskapare och svensk livsmedels- och dryckesdatabas.

## Ladda ner appen som APK från GitHub

Varje gång kod pushas till `main`/`master` (eller när arbetsflödet startas manuellt) bygger GitHub Actions automatiskt en färdig, signerad Android-APK.

### Alternativ 1: Direkt via GitHub Releases (enklast på mobilen)
1. Öppna **Releases** i högerspalten på GitHub-sidan (eller fliken **Releases**).
2. Välj **Zevra – Senaste APK** (`latest`).
3. Ladda ner **`Zevra.apk`** under **Assets** och öppna filen på din Android-telefon för att installera.

### Alternativ 2: Via GitHub Actions Artifacts
1. Öppna fliken **Actions** i ditt GitHub-repository.
2. Välj arbetsflödet **Bygg och publicera Android APK** (du kan också trycka på **Run workflow** för att starta ett nytt bygge när som helst).
3. Öppna den senaste gröna körningen och ladda ner **`Zevra-APK`** längst ner under **Artifacts**.

### (Valfritt) Firebase och egen release-signering
Arbetsflödet signerar `Zevra.apk` med projektets `debug.keystore.base64` (eller GitHub Secret `DEBUG_KEYSTORE_BASE64`) och verifierar automatiskt att nyckelns SHA-1 matchar `google-services.json` så att Google-inloggningen fungerar.

Om `google-services.json` eller `debug.keystore.base64` inte följde med när projektet pushades till GitHub lägger du in dem under **Settings → Secrets and variables → Actions** i GitHub:
- `GOOGLE_SERVICES_JSON` (hela JSON-innehållet från Firebase `google-services.json`)
- `DEBUG_KEYSTORE_BASE64` (innehållet i `debug.keystore.base64`, krävs om filen saknas i repot så att Google-inloggningens SHA-1 stämmer)

Om du även vill att `Zevra-release.apk` ska byggas med en egen release-nyckel lägger du till följande under **Secrets** (och registrerar release-nyckelns SHA-1/SHA-256 i Firebase Console):
- `KEYSTORE_BASE64` (base64-kodad `.jks`-fil med alias `upload`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
