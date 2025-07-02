# BlueChat 📱

یک اپلیکیشن پیام‌رسان بلوتوث پیشرفته با قابلیت چت متنی و تماس صوتی real-time

## 🌟 ویژگی‌ها

### 💬 چت متنی
- ارسال و دریافت پیام‌های متنی
- ذخیره‌سازی تاریخچه چت
- رابط کاربری زیبا و کاربرپسند

### 📞 تماس صوتی Real-time
- تماس صوتی دوطرفه با کیفیت بالا
- انتقال صدا بدون تأخیر
- قابلیت شروع/توقف تماس در حین چت

### 🔗 اتصال بلوتوث
- اسکن خودکار دستگاه‌های اطراف
- اتصال آسان و سریع
- نمایش وضعیت اتصال

## 📱 نیازمندی‌ها

- Android 6.0 (API level 23) یا بالاتر
- بلوتوث فعال
- مجوزهای میکروفون و بلوتوث

## 🚀 نصب

### روش ۱: دانلود مستقیم
1. فایل `app-release.apk` را دانلود کنید
2. روی فایل کلیک کنید تا نصب شود
3. مجوزهای مورد نیاز را تأیید کنید

### روش ۲: ساخت از کد منبع
```bash
# کلون کردن پروژه
git clone https://github.com/MiladPezeshkian/BlueChat.git
cd BlueChat

# ساخت پروژه
./gradlew assembleRelease
```

## 📖 نحوه استفاده

### مرحله ۱: فعال‌سازی بلوتوث
- برنامه را باز کنید
- روی دکمه "فعال‌سازی بلوتوث" کلیک کنید

### مرحله ۲: اسکن دستگاه‌ها
- روی "شروع اسکن" کلیک کنید
- منتظر بمانید تا دستگاه‌های اطراف پیدا شوند

### مرحله ۳: انتخاب دستگاه
- دستگاه مورد نظر را از لیست انتخاب کنید
- منتظر اتصال بمانید

### مرحله ۴: شروع چت یا تماس
- برای چت: روی آیکون پیام کلیک کنید
- برای تماس صوتی: روی آیکون بلوتوث کلیک کنید

## 🛠️ تکنولوژی‌های استفاده شده

- **زبان برنامه‌نویسی**: Java
- **پلتفرم**: Android Native
- **پایگاه داده**: Room Database
- **اتصال**: Bluetooth RFCOMM
- **صوت**: AudioRecord & AudioTrack
- **UI**: Material Design Components

## 📁 ساختار پروژه

```
app/
├── src/main/
│   ├── java/com/lonewalker/bluetoothmessenger/
│   │   ├── adapters/          # آداپترهای RecyclerView
│   │   ├── data/              # کلاس‌های داده و پایگاه داده
│   │   ├── ui/                # Activity ها
│   │   └── voice/             # مدیریت صدا
│   ├── res/
│   │   ├── layout/            # فایل‌های layout
│   │   ├── drawable/          # آیکون‌ها و تصاویر
│   │   └── values/            # رنگ‌ها، متن‌ها و تم‌ها
│   └── AndroidManifest.xml    # تنظیمات برنامه
└── build.gradle               # تنظیمات Gradle
```

## 🔧 تنظیمات

### مجوزهای مورد نیاز
```xml
<uses-permission android:name="android.permission.BLUETOOTH" />
<uses-permission android:name="android.permission.BLUETOOTH_ADMIN" />
<uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
<uses-permission android:name="android.permission.BLUETOOTH_SCAN" />
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
```

## 🐛 عیب‌یابی

### مشکل: دستگاه‌ها پیدا نمی‌شوند
- بلوتوث را فعال کنید
- دستگاه مقابل را قابل مشاهده کنید
- مجوز موقعیت مکانی را تأیید کنید

### مشکل: صدا کار نمی‌کند
- مجوز میکروفون را تأیید کنید
- مطمئن شوید که میکروفون دستگاه سالم است
- اتصال بلوتوث را بررسی کنید

### مشکل: پیام‌ها ارسال نمی‌شوند
- اتصال بلوتوث را بررسی کنید
- برنامه را مجدداً راه‌اندازی کنید
- دستگاه مقابل را مجدداً متصل کنید

## 🤝 مشارکت

از مشارکت شما استقبال می‌کنیم! برای مشارکت:

1. پروژه را Fork کنید
2. یک شاخه جدید ایجاد کنید (`git checkout -b feature/AmazingFeature`)
3. تغییرات را Commit کنید (`git commit -m 'Add some AmazingFeature'`)
4. به شاخه Push کنید (`git push origin feature/AmazingFeature`)
5. یک Pull Request ایجاد کنید

## 📄 لایسنس

این پروژه تحت لایسنس MIT منتشر شده است. برای اطلاعات بیشتر فایل `LICENSE` را مطالعه کنید.

## 👨‍💻 توسعه‌دهنده

**Milad Pezeshkian**
- GitHub: [@MiladPezeshkian](https://github.com/MiladPezeshkian)

## 📞 پشتیبانی

اگر سوال یا مشکلی دارید، لطفاً یک Issue ایجاد کنید یا با ایمیل تماس بگیرید.

---

⭐ اگر این پروژه برایتان مفید بود، لطفاً آن را ستاره‌دار کنید! #
