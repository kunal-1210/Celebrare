plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")

}

android {
    namespace = "com.example.celebrare"
    compileSdk = 36
    viewBinding {
        enable = true
    }


    defaultConfig {
        android.buildFeatures.buildConfig = true
        applicationId = "com.example.celebrare"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val firebaseUrl: String = project.findProperty("FIREBASE_DB_URL") as? String ?: ""
        buildConfigField("String", "FIREBASE_DB_URL", "\"$firebaseUrl\"")

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
}

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation ("androidx.core:core:1.12.0") // latest androidx core
    implementation ("androidx.appcompat:appcompat:1.7.0")

    implementation("com.google.firebase:firebase-auth:22.3.1")
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    implementation("com.google.firebase:firebase-database:20.3.3")

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.firebase.database)
    implementation(libs.ui.graphics)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}