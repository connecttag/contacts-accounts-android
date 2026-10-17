plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.serialization.plugin)
    `maven-publish`
}

group = "org.connecttag.libs.kotlin"
version = "1.0.0"

android {
    namespace = "org.connecttag.features.contactsaccounts.android"
    compileSdk = 36

    defaultConfig {
        minSdk = 24
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
        }
    }
}

dependencies {
    implementation(libs.androidx.ktx)
    implementation(libs.androidx.annotation.jvm)
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.serialization.json)
}

afterEvaluate {
    publishing {
        publications {
            create<MavenPublication>("release") {
                from(components["release"])
                groupId = "org.connecttag.libs.kotlin"
                artifactId = "contacts-accounts-android"
                version = "1.0.0"

                pom {
                    name.set("ConnectTag Contacts & Accounts Android")
                    description.set("Android library for querying, managing, and synchronizing device contacts and system accounts with backup and deduplication support")
                    url.set("https://github.com/connecttag/contacts-accounts-android")
                    licenses {
                        license {
                            name.set("The Apache License, Version 2.0")
                            url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        }
                    }
                    developers {
                        developer {
                            id.set("connecttagye")
                            name.set("Connect Tag")
                            email.set("connecttagye@gmail.com")
                        }
                    }
                    scm {
                        connection.set("scm:git:https://github.com/connecttag/contacts-accounts-android.git")
                        developerConnection.set("scm:git:ssh://git@github.com:connecttag/contacts-accounts-android.git")
                        url.set("https://github.com/connecttag/contacts-accounts-android/tree/main")
                    }
                }
            }
        }
    }
}
