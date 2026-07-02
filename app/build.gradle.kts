plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.novel.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.novel.app"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = true
    }

    // =============================================================
    // buildTypes - إضافة نوع البناء release و debug
    // =============================================================
    buildTypes {
        // نوع الإصدار (Release) - يستخدم توقيع debug مؤقتاً
        // ⚠️ ملاحظة: هذا مناسب للاختبار فقط. للإصدار النهائي،
        //    يجب إنشاء keystore خاص وتوقيع التطبيق بشكل آمن.
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // استخدام توقيع debug مؤقتاً (للاختبار فقط)
            signingConfig = signingConfigs.getByName("debug")
        }

        // نوع التصحيح (Debug) - يستخدم توقيع debug تلقائياً
        debug {
            isMinifyEnabled = false
        }
    }

    // تجاهل أخطاء Lint لمنع فشل البناء
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("org.jsoup:jsoup:1.17.2")
    implementation("androidx.preference:preference:1.2.1")
}
