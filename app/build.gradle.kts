import org.gradle.kotlin.dsl.implementation

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.newsproject.oneroadmap"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.newsproject.oneroadmap"
        minSdk = 24
        targetSdk = 36
        versionCode = 4
        versionName = "1.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures{
        viewBinding = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }



    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.circleimageview)
    implementation(libs.glide)
    implementation(libs.circularstatusview)
    implementation(libs.whynotimagecarousel)
    implementation(libs.exoplayer)
    implementation(libs.swiperefreshlayout)
    implementation(libs.dotsindicator)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.gson)
    implementation(libs.cardview)
    implementation(libs.firebase.crashlytics.buildtools)
    implementation(libs.legacy.support.v4)
    implementation(libs.recyclerview)
    implementation("androidx.viewpager2:viewpager2:1.1.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation("com.squareup.okhttp3:okhttp:5.2.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.mikhaellopez:circularprogressbar:3.1.0")
    implementation("com.google.android.gms:play-services-ads:23.6.0")
}