# NäringAI

En Android-app (Kotlin & Jetpack Compose) för kostspårning, AI-måltidsanalys, träning/Health Connect, receptskapare och svensk livsmedels- och dryckesdatabas.

## Ladda ner appen som APK från GitHub

Varje gång kod pushas till `main`/`master` (eller när arbetsflödet startas manuellt) bygger GitHub Actions automatiskt en färdig, signerad Android-APK.

### Alternativ 1: Direkt via GitHub Releases (enklast på mobilen)
1. Öppna **Releases** i högerspalten på GitHub-sidan (eller fliken **Releases**).
2. Välj **NäringAI – Senaste APK** (`latest`).
3. Ladda ner **`NaringAI.apk`** under **Assets** och öppna filen på din Android-telefon för att installera.

### Alternativ 2: Via GitHub Actions Artifacts
1. Öppna fliken **Actions** i ditt GitHub-repository.
2. Välj arbetsflödet **Bygg och publicera Android APK** (du kan också trycka på **Run workflow** för att starta ett nytt bygge när som helst).
3. Öppna den senaste gröna körningen och ladda ner **`NaringAI-APK`** längst ner under **Artifacts**.

### (Valfritt) Egen release-signering
Arbetsflödet signerar automatiskt `NaringAI.apk` med projektets inbyggda `debug.keystore.base64` så att både installation och Firebase fungerar direkt. Om du även vill att `NaringAI-release.apk` ska byggas med en egen release-nyckel lägger du till följande under **Settings → Secrets and variables → Actions** i GitHub:
- `KEYSTORE_BASE64` (base64-kodad `.jks`-fil med alias `upload`)
- `STORE_PASSWORD`
- `KEY_PASSWORD`
