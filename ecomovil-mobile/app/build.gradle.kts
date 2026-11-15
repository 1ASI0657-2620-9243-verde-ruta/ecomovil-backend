plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "pe.verderuta.ecomovil"
    compileSdk = 35

    defaultConfig {
        applicationId = "pe.verderuta.ecomovil"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    // La app empaqueta el mismo frontend web del repositorio (../../ecomovil-web),
    // así web y móvil comparten una sola base de código.
    sourceSets {
        getByName("main") {
            assets.srcDirs("src/main/assets", "../../ecomovil-web")
        }
    }
}

dependencies {
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.webkit:webkit:1.12.1")
}
