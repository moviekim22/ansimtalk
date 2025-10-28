plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // Retrofit: 서버 통신을 위한 라이브러리
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    // Gson Converter: 서버와 주고받는 데이터(JSON)를 코틀린 객체로 변환해주는 도구
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp Logging Interceptor: 통신 과정을 로그로 확인하기 위한 도구 (개발에 매우 유용)
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")


    // Google Play 서비스의 위치 정보 라이브러리
    implementation("com.google.android.gms:play-services-location:21.2.0")

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    // Material Icons Extended 추가
    implementation("androidx.compose.material:material-icons-extended-android:1.6.8")

    // Jetpack Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.7.7")

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}