plugins {
    alias(libs.plugins.android.application)

    // VỊ TRÍ 1: Thêm plugin Google Services vào đây
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.friendmap"
    compileSdk = 36 // Đưa về định dạng chuẩn gọn gàng cho compileSdk

    defaultConfig {
        applicationId = "com.example.friendmap"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false // Sửa lại thuộc tính proguard/minify chuẩn cho build.gradle.kts
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

    // VỊ TRÍ 2: Thêm cụm thư viện Firebase sử dụng BoM vào đây
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
}