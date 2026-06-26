plugins {
<<<<<<< HEAD
    id("com.android.application")
=======
    alias(libs.plugins.android.application)

    // VỊ TRÍ 1: Thêm plugin Google Services vào đây
>>>>>>> friend/master
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.friendmap"
<<<<<<< HEAD
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.friendmap"
        minSdk = 26
=======
    compileSdk = 36 // Đưa về định dạng chuẩn gọn gàng cho compileSdk

    defaultConfig {
        applicationId = "com.example.friendmap"
        minSdk = 29
>>>>>>> friend/master
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
<<<<<<< HEAD
            isMinifyEnabled = false
=======
            isMinifyEnabled = false // Sửa lại thuộc tính proguard/minify chuẩn cho build.gradle.kts
>>>>>>> friend/master
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
<<<<<<< HEAD
=======
    implementation(libs.activity.ktx)
>>>>>>> friend/master
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)

<<<<<<< HEAD
    // Firebase
=======
    // VỊ TRÍ 2: Thêm cụm thư viện Firebase sử dụng BoM vào đây
>>>>>>> friend/master
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-database")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-messaging")
<<<<<<< HEAD
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("com.google.android.gms:play-services-location:21.2.0")
=======
>>>>>>> friend/master
}