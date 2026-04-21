plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.ethiobalance.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ethiobalance.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Property injection
        buildConfigField("String", "USSD_BALANCE_CHECK", "\"${project.findProperty("ethiobalance.ussd.balance_check") ?: "*804#"}\"")
        buildConfigField("String", "USSD_RECHARGE_SELF", "\"${project.findProperty("ethiobalance.ussd.recharge_self") ?: "*805*"}\"")
        buildConfigField("String", "USSD_RECHARGE_OTHER", "\"${project.findProperty("ethiobalance.ussd.recharge_other") ?: "*805*"}\"")
        buildConfigField("String", "USSD_TRANSFER_AIRTIME", "\"${project.findProperty("ethiobalance.ussd.transfer_airtime") ?: "*806*"}\"")
        buildConfigField("String", "USSD_GIFT_PACKAGE", "\"${project.findProperty("ethiobalance.ussd.gift_package") ?: "*999#"}\"")
        buildConfigField("String", "PHONE_APP_PACKAGE", "\"${project.findProperty("ethiobalance.phone_app_package") ?: "com.android.phone"}\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

dependencies {
    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose UI
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Compose integration
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Google Fonts for Compose (Manrope)
    implementation("androidx.compose.ui:ui-text-google-fonts")

    // AndroidX Core
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("androidx.core:core-splashscreen:1.0.1")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    // Room persistence library
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // DataStore for preferences
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Hilt DI
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}
