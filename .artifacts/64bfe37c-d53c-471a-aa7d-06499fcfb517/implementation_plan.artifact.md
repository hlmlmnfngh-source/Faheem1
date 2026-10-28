# إنشاء واجهات تطبيق "فهيم" (Splash & Terms)

سيتم إنشاء واجهتين جديدتين للتطبيق بناءً على الصور المرفقة، مع كود الجافا الخاص بكل منهما.

## Proposed Changes

### 1. الموارد (Resources)

#### [MODIFY] [colors.xml](file:///D:/Android Project/app/src/main/res/values/colors.xml)
إضافة الألوان المتبقية اللازمة للتصميم.

#### [MODIFY] [strings.xml](file:///D:/Android Project/app/src/main/res/values/strings.xml)
إضافة النصوص العربية للواجهتين.

#### [NEW] [circle_bg.xml](file:///D:/Android Project/app/src/main/res/drawable/circle_bg.xml)
خلفية دائرية لشعار الشاشة الافتتاحية.

#### [NEW] [card_bg.xml](file:///D:/Android Project/app/src/main/res/drawable/card_bg.xml)
خلفية بيضاء بزوايا مستديرة لبطاقة الشروط.

#### [NEW] [button_bg.xml](file:///D:/Android Project/app/src/main/res/drawable/button_bg.xml)
خلفية الزر (رمادي أو أخضر حسب الحالة).

---

### 2. الشاشة الافتتاحية (Splash Screen)

#### [NEW] [activity_splash.xml](file:///D:/Android Project/app/src/main/res/layout/activity_splash.xml)
تصميم الواجهة التي تحتوي على الشعار، اسم التطبيق "فهيم"، والوصف.

#### [NEW] [SplashActivity.java](file:///D:/Android Project/app/src/main/java/com/example/faheem/SplashActivity.java)
كود الجافا للتحكم في الانتقال من الشاشة الافتتاحية إلى شاشة الشروط.

---

### 3. شاشة الشروط والتعليمات (Terms & Conditions)

#### [NEW] [activity_terms.xml](file:///D:/Android Project/app/src/main/res/layout/activity_terms.xml)
تصميم واجهة "قبل ما نبلش" مع قائمة التعليمات وصندوق الموافقة.

#### [NEW] [TermsActivity.java](file:///D:/Android Project/app/src/main/java/com/example/faheem/TermsActivity.java)
كود الجافا للتحقق من الموافقة على الشروط وتفعيل زر المتابعة.

---

### 4. الإعدادات (Configuration)

#### [MODIFY] [AndroidManifest.xml](file:///D:/Android Project/app/src/main/AndroidManifest.xml)
تسجيل الأنشطة الجديدة وتحديد `SplashActivity` كشاشة البداية.

## Verification Plan

### Manual Verification
1. تشغيل التطبيق والتأكد من ظهور الشاشة الافتتاحية أولاً.
2. التأكد من الانتقال التلقائي (أو بعد وقت محدد) إلى شاشة الشروط.
3. في شاشة الشروط، التأكد من أن زر "أوافق وأتابع" لا يعمل إلا بعد تفعيل خيار "قرأت ووافقت".
