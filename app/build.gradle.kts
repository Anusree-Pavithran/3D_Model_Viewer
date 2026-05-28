plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.a3dmodelviewer"
    buildFeatures {
        viewBinding = true
    }
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }
    signingConfigs {
        create("3dmodelviewer") {
            keyAlias = "123123"
            keyPassword = "123123"
            storeFile = file("../keystore/3Dkeystore")
            storePassword = "123123"
        }
    }
    defaultConfig {
        applicationId = "com.example.a3dmodelviewer"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

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
}

dependencies {
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)

    implementation("io.github.sceneview:sceneview:2.2.1")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}