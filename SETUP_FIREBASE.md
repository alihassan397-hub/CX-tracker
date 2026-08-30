# CX Tracker — Firebase Setup (one-time)

Code mein backend already wire ho chuka hai (Firestore + Firebase Auth +
security rules + Cloud Functions). Yeh sirf woh steps hain jo aapko khud
apne Google account se karne hain — yeh main remotely nahi kar sakta.

## 1. Firebase project banayein
1. https://console.firebase.google.com par jayein → **Add project**
2. Naam dein (e.g. "CX Tracker") → free **Spark** plan se shuru karein
   (Cloud Functions ke liye baad mein **Blaze** plan par upgrade karna hoga —
   Blaze mein bhi free quota hai, chhoti team ke liye bill $0 rahega)

## 2. Android app add karein
1. Project settings → **Add app** → Android
2. Package name: `com.aistudio.hblcxtracker.pk7m2` (ye already `build.gradle.kts` mein set hai)
3. `google-services.json` download karein
4. Us file ko `app/google-services.json` mein rakhein (root nahi, **app/** folder ke andar)

## 3. Authentication enable karein
1. Firebase Console → **Build → Authentication → Get started**
2. **Email/Password** provider ko enable karein

## 4. Firestore enable karein
1. Firebase Console → **Build → Firestore Database → Create database**
2. Production mode select karein (rules already is repo mein hain)
3. Region select karein (jo aapke users ke qareeb ho)

## 5. Security Rules deploy karein
Apne computer par (jahan Node.js installed ho):
```bash
npm install -g firebase-tools
firebase login
cd cx-tracker-patched     # is project ki root directory
firebase use --add        # apna Firebase project select karein
firebase deploy --only firestore:rules
```

## 6. Cloud Functions deploy karein (auto email + TAT check)
Email bhejne ke liye ek Gmail "App Password" chahiye hoga (Google Account →
Security → 2-Step Verification → App Passwords se generate karein — apna
normal Gmail password mat use karein).

```bash
cd functions
npm install
cd ..
firebase functions:secrets:set SMTP_USER
firebase functions:secrets:set SMTP_PASS
firebase deploy --only functions
```

## 7. Pehla Unit Head account banayein
Security ki wajah se **koi bhi sign-up khud Unit Head nahi ban sakta** — yeh
jaan-boojh kar aisa design kiya gaya hai. Pehla admin account banane ka
tareeqa:

1. Sabeen Shafique apni asal email/password se app mein normally **sign up**
   karein (wo Team Member ban jayengi, ye normal hai)
2. Aap khud Firebase Console → **Firestore Database → users** collection
   mein jayein, unki document dhoondein, aur `role` field ko
   `TEAM_MEMBER` se **`UNIT_HEAD`** mein manually change kar dein
3. Ab agli baar login karte hi wo Unit Head ban jayengi — pura app unhi ko
   admin dikhayega

Is ke baad koi bhi naya Unit Head banane ke liye same manual Firestore step
repeat karein — yeh jaan-boojh kar app ke bahar rakha gaya hai taake koi
bhi self-service se admin na ban sake.

## 8. App build karein
GitHub repo mein push karein — `.github/workflows/build_apk.yml` khud APK
build kar dega (ab woh `google-services.json` ko bhi include karna hoga,
ya CI secrets mein base64 encode kar ke pass karein — agar madad chahiye to
bataiyega, main workflow file bhi adjust kar dunga).

## Verify checklist
- [ ] `app/google-services.json` maujood hai
- [ ] Authentication → Email/Password enabled hai
- [ ] Firestore Database bana hua hai
- [ ] `firebase deploy --only firestore:rules` chal chuka hai
- [ ] `firebase deploy --only functions` chal chuka hai (SMTP secrets set karne ke baad)
- [ ] Sabeen ka account bana hai aur role manually UNIT_HEAD kiya gaya hai
