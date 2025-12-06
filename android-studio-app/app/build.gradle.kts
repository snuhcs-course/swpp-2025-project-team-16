plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
    id("jacoco")


}

android {
    namespace = "com.fitquest.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.fitquest.app"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName("debug") {
            // 최신 AGP에서는 testCoverageEnabled 제거됨
            // rootcoverage 플러그인이 커버리지 수집을 담당
            isTestCoverageEnabled=true
        }
        debug{
            isMinifyEnabled=false
        }
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
        dataBinding = true
    }

    testOptions {
        managedDevices {
            devices {
                // Kotlin DSL에서는 create<ManagedVirtualDevice>("이름") 으로 정의
                create<com.android.build.api.dsl.ManagedVirtualDevice>("pixel6") {
                    device = "Pixel 6"
                    apiLevel = 33
                    systemImageSource = "google-atd"
                }
            }
        }


        unitTests {
            isIncludeAndroidResources = true
        }
    }

    tasks.register<JacocoReport>("jacocoAndroidTestReport") {
        dependsOn(":app:connectedDebugAndroidTest")

        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val fileFilter = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*"
        )

        // Kotlin 클래스와 Java 클래스 디렉토리 모두 잡아줍니다
        val kotlinDebugTree = fileTree("${project(":app").buildDir}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }
        val javaDebugTree = fileTree("${project(":app").buildDir}/intermediates/javac/debug/classes") {
            exclude(fileFilter)
        }


        val mainSrc = "${project(":app").projectDir}/src/main/java"

        sourceDirectories.setFrom(files(mainSrc))
        classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))

        // 여기서 coverage.ec 파일을 읽어옵니다
        executionData.setFrom(fileTree("${project(":app").buildDir}/outputs/code_coverage/debugAndroidTest/connected/SM-S918N - 13") {
            include("coverage.ec")
        })
    }

    tasks.register<JacocoReport>("pixel6DebugAndroidTestCoverage") {
        reports {
            xml.required.set(true)
            html.required.set(true)
        }

        val fileFilter = listOf(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*",
            "**/*Test*.*"
        )

        // Kotlin 클래스와 Java 클래스 디렉토리 모두 잡아줍니다
        val kotlinDebugTree = fileTree("${project(":app").buildDir}/tmp/kotlin-classes/debug") {
            exclude(fileFilter)
        }
        val javaDebugTree = fileTree("${project(":app").buildDir}/intermediates/javac/debug/classes") {
            exclude(fileFilter)
        }


        val mainSrc = "${project(":app").projectDir}/src/main/java"

        sourceDirectories.setFrom(files(mainSrc))
        classDirectories.setFrom(files(kotlinDebugTree, javaDebugTree))

        // 여기서 coverage.ec 파일을 읽어옵니다
        executionData.setFrom(fileTree(rootProject.projectDir) {
            include("**/coverage.ec")
        })

    }

}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.6")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.6")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.coordinatorlayout:coordinatorlayout:1.2.0")
    implementation("com.google.android.material:material:1.12.0")

    // ThreeTenABP (java.time backport)
    implementation("com.jakewharton.threetenabp:threetenabp:1.4.4")

    // Image loading
    implementation("com.github.bumptech.glide:glide:4.16.0")

    // ViewPager2
    implementation("androidx.viewpager2:viewpager2:1.0.0")

    // Fragment
    implementation("androidx.fragment:fragment-ktx:1.6.2")

    // LiveData and ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")

    // CameraX dependencies for AI pose analysis
    implementation("androidx.camera:camera-core:1.3.1")
    implementation("androidx.camera:camera-camera2:1.3.1")
    implementation("androidx.camera:camera-lifecycle:1.3.1")
    implementation("androidx.camera:camera-video:1.3.1")
    implementation("androidx.camera:camera-view:1.3.1")
    implementation("androidx.camera:camera-extensions:1.3.1")
    // ML Kit / MediaPipe 에서 protobuf 전부 제거
    implementation("com.google.mediapipe:tasks-vision:0.10.29") {
        exclude(group = "com.google.protobuf") // javalite + lite 모두
    }

    implementation("com.google.mlkit:pose-detection:18.0.0-beta3") {
        exclude(group = "com.google.protobuf")
    }
    implementation("com.google.mlkit:pose-detection-accurate:18.0.0-beta3") {
        exclude(group = "com.google.protobuf")
    }

    // 우리가 직접 넣는 protobuf 버전(앱 + 테스트 공통)
    implementation("com.google.protobuf:protobuf-javalite:3.21.12")
    testImplementation("com.google.protobuf:protobuf-javalite:3.21.12")
    androidTestImplementation("com.google.protobuf:protobuf-javalite:3.21.12")

    // Permissions
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("com.google.android.material:material:1.13.0")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("junit:junit:4.13.2")
    testImplementation("junit:junit:4.12")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation ("androidx.test.espresso:espresso-intents:3.5.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.5.1") {
        exclude(group = "com.google.protobuf", module = "protobuf-lite")
    }
    androidTestImplementation ("androidx.test.espresso:espresso-idling-resource:3.5.1")


    androidTestImplementation ("androidx.fragment:fragment-testing:1.6.2")


    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    androidTestImplementation("org.mockito:mockito-android:5.2.0")
    androidTestImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.mockito:mockito-core:5.10.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.2.1")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("com.squareup.okhttp3:okhttp:4.12.0")

    // --- ✅ Kotlin Coroutines 테스트 ---
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")

    // --- ✅ AndroidX Test Core ---
    testImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test:core:1.5.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")

    // --- ✅ Robolectric ---
    testImplementation("org.robolectric:robolectric:4.12.2")

    // --- ✅ Fragment / Activity 테스트 지원 (optional but recommended) ---
    testImplementation("androidx.fragment:fragment-testing:1.8.2")
    testImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")

    // --- CalendarView 지원
    implementation("com.prolificinteractive:material-calendarview:1.4.3")

    implementation("com.google.android.material:material:1.11.0")

    // For confetti
    implementation("nl.dionsegijn:konfetti-xml:2.0.2")
}
configurations.all {
    resolutionStrategy.eachDependency {
        // ✅ javalite 에만 3.21.12 강제 적용
        if (requested.group == "com.google.protobuf" &&
            requested.name == "protobuf-javalite"
        ) {
            useVersion("3.21.12")
        }
    }
}