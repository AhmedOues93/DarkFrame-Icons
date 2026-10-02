plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.darkframe.icons"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.darkframe.icons"
        minSdk = 26
        targetSdk = 35
        // Bumped together: the optical-correction pass changes every rendered icon, the six
        // collections gained their own materials, and the widget set went from three to six.
        versionCode = 3
        versionName = "1.1"
        resourceConfigurations += listOf("en")
    }

    signingConfigs {
        // Release signing is supplied by the person running the build, never by the repository.
        // Put the four values in ~/.gradle/gradle.properties or pass them with -P; see
        // docs/RELEASE_SIGNING.md. With none of them set, a release build is simply unsigned, which
        // is what CI wants and what keeps keys out of version control.
        create("release") {
            val storePath = providers.gradleProperty("DARKFRAME_STORE_FILE").orNull
            if (storePath != null) {
                storeFile = file(storePath)
                storePassword = providers.gradleProperty("DARKFRAME_STORE_PASSWORD").orNull
                keyAlias = providers.gradleProperty("DARKFRAME_KEY_ALIAS").orNull
                keyPassword = providers.gradleProperty("DARKFRAME_KEY_PASSWORD").orNull
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (providers.gradleProperty("DARKFRAME_STORE_FILE").isPresent) {
                signingConfigs.getByName("release")
            } else {
                null
            }
        }
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        viewBinding = false
        buildConfig = true
    }

    lint {
        abortOnError = true
        warningsAsErrors = false
        checkDependencies = false
        lintConfig = file("lint.xml")
        // Printed to the build log as well as the HTML report, so warnings are visible in CI
        // output instead of only inside a downloadable artifact nobody opens.
        textReport = true
        textOutput = file("stdout")
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.3")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("com.google.android.material:material:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("com.android.billingclient:billing-ktx:7.1.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
