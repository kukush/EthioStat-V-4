import org.gradle.testing.jacoco.plugins.JacocoTaskExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("jacoco")
    id("io.gitlab.arturbosch.detekt")
}

detekt {
    buildUponDefaultConfig = true
    ignoreFailures = false
    toolVersion = "1.23.6"
    basePath = rootDir.absolutePath
    config = files("$rootDir/config/detekt/detekt.yml")
    baseline = file("$rootDir/config/detekt/detekt-baseline.xml")
}

jacoco {
    toolVersion = "0.8.13"
}

android {
    namespace = "com.ethiobalance.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ethiobalance.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Property injection
        buildConfigField("String", "USSD_BALANCE_CHECK", "\"${project.findProperty("ethiobalance.ussd.balance_check") ?: "*804#"}\"")
        buildConfigField("String", "USSD_RECHARGE_SELF", "\"${project.findProperty("ethiobalance.ussd.recharge_self") ?: "*805*"}\"")
        buildConfigField("String", "USSD_RECHARGE_OTHER", "\"${project.findProperty("ethiobalance.ussd.recharge_other") ?: "*805*"}\"")
        buildConfigField("String", "USSD_TRANSFER_AIRTIME", "\"${project.findProperty("ethiobalance.ussd.transfer_airtime") ?: "*806*"}\"")
        buildConfigField("String", "USSD_GIFT_PACKAGE", "\"${project.findProperty("ethiobalance.ussd.gift_package") ?: "*999#"}\"")
        buildConfigField("String", "DEFAULT_TRANSACTION_SOURCES", "\"${project.findProperty("ethiobalance.default_sources") ?: "CBE,TELEBIRR"}\"")
    }

    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    applicationVariants.configureEach {
        outputs.configureEach {
            val output = this as com.android.build.gradle.internal.api.ApkVariantOutputImpl
            output.outputFileName = "EthioStat-${name}.apk"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    // Enable Android resources in local unit tests so AndroidJUnit4 can be used
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
}

tasks.withType<Test>().configureEach {
    extensions.configure<JacocoTaskExtension> {
        isIncludeNoLocationClasses = true
        excludes = listOf("jdk.internal.*")
    }
}

val coverageExclusions = listOf(
    "**/R.class",
    "**/R$*.class",
    "**/BuildConfig.*",
    "**/Manifest*.*",
    "**/*Test*.*",
    "**/*\$*",
    "**/*ComposableSingletons*.*",
    "**/Hilt_*.*",
    "**/*_HiltModules*.*",
    "**/*_Factory.*",
    "**/*_Impl.*",
    "**/*_MembersInjector.*",
    "**/*Module_*Factory.*",
    "**/*Component*.*",
    "**/*Dagger*.*",
    "**/dagger/hilt/**",
    "**/hilt_aggregated_deps/**"
)

tasks.register<JacocoReport>("jacocoDebugUnitTestReport") {
    group = "verification"
    description = "Generates JaCoCo HTML and XML coverage reports for debug unit tests."

    dependsOn("testDebugUnitTest")

    reports {
        html.required.set(true)
        html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoDebugUnitTestReport/html"))
        xml.required.set(true)
        xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoDebugUnitTestReport/jacocoDebugUnitTestReport.xml"))
        csv.required.set(false)
    }

    sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
    classDirectories.setFrom(
        fileTree(layout.buildDirectory.dir("intermediates/classes/debug/transformDebugClassesWithAsm/dirs")) {
            exclude(coverageExclusions)
        }
    )
    executionData.setFrom(
        files(
            layout.buildDirectory.file("jacoco/testDebugUnitTest.exec")
        )
    )
}

afterEvaluate {
    tasks.named("testDebugUnitTest") {
        finalizedBy("jacocoDebugUnitTestReport")
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
    implementation("androidx.lifecycle:lifecycle-process:2.8.7")
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
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("app.cash.turbine:turbine:1.1.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    // Add AndroidX JUnit4 runner for unit tests that reference AndroidJUnit4
    // AndroidX test libraries required for unit tests that use AndroidJUnit4
    // AndroidX test libraries for unit tests that rely on AndroidJUnit4
    testImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.test:runner:1.5.2")
    testImplementation("androidx.test:core:1.5.0")
    testImplementation("org.robolectric:robolectric:4.11.1")
    // Also keep them for instrumented (androidTest) usage
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
}

// -----------------------------------------------------------------------------
// Transaction Source Migration Generator
// -----------------------------------------------------------------------------
val generateSourceMigration by tasks.registering {
    val inputFile = file("src/main/java/com/ethiobalance/app/AppConstants.kt")
    val outputDir = layout.buildDirectory.dir("generated/source/migration/com/ethiobalance/app/data")
    val outputFile = outputDir.map { it.file("GeneratedSourceMigration.kt") }

    inputs.file(inputFile)
    outputs.dir(outputDir)

    doLast {
        val content = inputFile.readText()
        val regex = Regex("""TransactionSourceDef\s*\(\s*abbreviation\s*=\s*"([^"]+)",\s*displayName\s*=\s*"([^"]+)",\s*senderIds\s*=\s*listOf\(([^)]+)\)(?:,\s*ussd\s*=\s*"([^"]*)")?""")
        
        val matches = regex.findAll(content)
        val sourcesCode = matches.map { match ->
            val abbreviation = match.groupValues[1]
            val name = match.groupValues[2]
            val sendersStr = match.groupValues[3].replace("\"", "").replace(" ", "")
            val ussd = if (match.groupValues.size > 4) match.groupValues[4] else ""
            
            """TransactionSourceEntity("$abbreviation", "$name", "$ussd", "$sendersStr", true, System.currentTimeMillis())"""
        }.joinToString(",\n                ")

        val generatedCode = """
package com.ethiobalance.app.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object SourceMigration {
    val MIGRATION_8_9 = object : Migration(8, 9) {
        override fun migrate(database: SupportSQLiteDatabase) {
            val sources = listOf(
                $sourcesCode
            )
            sources.forEach { src ->
                database.execSQL(""${'"'}
                    INSERT OR IGNORE INTO transaction_sources
                    (abbreviation, name, ussd, senderId, isEnabled, lastUpdated)
                    VALUES ('${'$'}{src.abbreviation}', '${'$'}{src.name}', '${'$'}{src.ussd}', '${'$'}{src.senderId}', 1, ${'$'}{src.lastUpdated})
                ""${'"'}.trimIndent())
            }
        }
    }
}
""".trimIndent()

        outputDir.get().asFile.mkdirs()
        outputFile.get().asFile.writeText(generatedCode)
    }
}

android.applicationVariants.all {
    val variantName = name
    tasks.named("generate${variantName.replaceFirstChar { it.uppercase() }}Sources") {
        dependsOn(generateSourceMigration)
    }
}

tasks.matching { it.name.startsWith("kapt") }.configureEach {
    dependsOn(generateSourceMigration)
}

android.sourceSets {
    getByName("main") {
        java.srcDir(layout.buildDirectory.dir("generated/source/migration"))
    }
}
