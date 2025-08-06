import java.util.Properties

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties") // Use rootProject.file to ensure it's project-level

if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { input ->
        localProperties.load(input)
    }
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.example.amazonwhat"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.amazonwhat"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


        // Retrieve secrets from local.properties and make them available in BuildConfig
        // Provide default values if the properties are not found (e.g., for CI servers)
        val krogerClientId = localProperties.getProperty("KROGER_CLIENT_ID") ?: ""
        val krogerClientSecret = localProperties.getProperty("KROGER_CLIENT_SECRET") ?: ""

        buildConfigField("String", "KROGER_CLIENT_ID", "\"$krogerClientId\"")
        buildConfigField("String", "KROGER_CLIENT_SECRET", "\"$krogerClientSecret\"")
    }

    buildFeatures {
        buildConfig = true // Ensures BuildConfig.java is generated
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0") // For parsing JSON responses
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3") // Optional: For logging network requests
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("com.codepath.libraries:asynchttpclient:2.2.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}