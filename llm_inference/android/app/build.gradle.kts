import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.google.mediapipe.examples.llminference"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.google.mediapipe.examples.llminference"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["appAuthRedirectScheme"] = ""

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        // Define the BuildConfig field
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }
        val hfAccessToken = properties.getProperty("HF_ACCESS_TOKEN", "")
        buildConfigField("String", "HF_ACCESS_TOKEN", "\"$hfAccessToken\"")

        // Add native library support
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
        }
        
        // Ensure native libraries are extracted
        packaging {
            jniLibs {
                useLegacyPackaging = true
            }
        }
        
        // Add manifest placeholders for better native library handling
        manifestPlaceholders["native_library_support"] = "true"
    }

    buildFeatures {
        buildConfig = true
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
        jniLibs {
            useLegacyPackaging = true
            pickFirsts += listOf(
                "**/libc++_shared.so",
                "**/libtensorflowlite_jni.so"
            )
            // Keep all native libraries, don't exclude missing ones
            keepDebugSymbols += listOf(
                "**/libpenguin.so",
                "**/libllm_inference_engine_jni.so"
            )
        }
    }
}

dependencies {

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")

    implementation ("com.google.mediapipe:tasks-genai:0.10.22")
    
    // Add specific native library support
    implementation("org.tensorflow:tensorflow-lite:2.13.0") {
        exclude(group = "org.tensorflow", module = "tensorflow-lite-api")
    }

    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("net.openid:appauth:0.11.1") // Add AppAuth for OAuth support
    implementation("androidx.security:security-crypto:1.0.0")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    val room_version = "2.7.2" // Using the version provided in the prompt
    implementation("androidx.room:room-runtime:$room_version")
    ksp("androidx.room:room-compiler:$room_version") // For Kotlin projects
    implementation("androidx.room:room-ktx:$room_version") // For Kotlin Extensions and Coroutines support
}
