plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)

    id("com.google.gms.google-services")
}

allprojects {
}

android {
    namespace = "com.nishantyadav.routesmart"

    compileSdk = 36

    defaultConfig {
        applicationId = "com.nishantyadav.routesmart"
        minSdk = 24
        targetSdk = 36
        versionCode = 10
        versionName = "1.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation("com.google.android.material:material:1.11.0")

    implementation("org.maplibre.gl:android-sdk:10.0.2")

    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))

    implementation("com.google.firebase:firebase-auth-ktx")

    implementation("com.google.firebase:firebase-database-ktx")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")

    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.google.android.gms:play-services-maps:18.2.0")

    implementation("com.google.android.gms:play-services-location:21.0.1")

    implementation("org.osmdroid:osmdroid-android:6.1.16")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")

    implementation("org.json:json:20230227")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
