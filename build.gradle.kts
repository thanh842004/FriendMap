plugins {
    alias(libs.plugins.android.application) apply false

    // Thêm dòng này vào để khai báo plugin Google Services
    id("com.google.gms.google-services") version "4.4.0" apply false
}