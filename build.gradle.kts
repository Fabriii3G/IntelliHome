// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false

}
repositories {
    google()
    mavenCentral()
    implementation 'com.sun.mail:android-mail:1.6.6'
    implementation 'com.sun.mail:android-activation:1.6.6'

}
