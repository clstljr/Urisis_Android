# Urisis Android

Urisis Android is a Kotlin-based Android application for a urinalysis testing workflow. The app is designed to guide a user from account access and device connection through a pre-test protocol, sample analysis, results review, advanced sensor-data viewing, and test-history access.

## Purpose

The project demonstrates an Android application that combines Bluetooth Low Energy device communication, account management, local data storage, biometric-authentication support, and a multi-step urinalysis user experience.

This project is a software prototype and should not be treated as a medical diagnostic device or as a replacement for professional medical advice.

## Main Features

- Splash screen and medical disclaimer
- User login and registration screens
- Account picker and account switching
- Main dashboard
- Bluetooth Low Energy device connection flow
- Pre-test protocol screen
- Waiting-for-device and sample-analysis screens
- Urinalysis test-results screen
- Advanced results and sensor-data screens
- Test-history screen
- Local storage for application data and preferences
- Biometric-authentication support in the Android project configuration
- Bluetooth permission handling for different Android versions

## Technologies

- Kotlin
- Android Studio
- Jetpack Compose
- Material 3
- Android SDK
- Room Database
- Kotlin Coroutines
- ViewModel and Lifecycle Compose
- DataStore Preferences
- Bluetooth Low Energy APIs
- Android Biometric APIs
- KSP for Room code generation

## Application Flow

1. The app opens with a splash screen.
2. A user reviews the medical disclaimer and signs in or registers.
3. The user reaches the dashboard and can connect to a BLE device.
4. The user starts the urinalysis process and follows the pre-test protocol.
5. The app waits for the device, analyzes the sample, and presents results.
6. The user can review advanced sensor data or save and open previous test records.

## Requirements

- Android Studio
- Android SDK 35
- Java Development Kit 21
- An Android device or emulator with Android 7.0/API 24 or newer
- A compatible Bluetooth Low Energy device for testing the device-connection workflow

The project is configured with `minSdk 24`, `targetSdk 35`, Java 21, and Jetpack Compose. The application also declares Bluetooth and biometric permissions. Check the Android permission prompts when running the app on a physical device.

## How to Run

Clone the repository:

```bash
git clone https://github.com/clstljr/Urisis_Android.git
```

Open the project in Android Studio, allow Gradle to sync, start an Android emulator or connect a compatible Android device, and press the **Run** button.

For BLE testing, a physical Android device and the intended BLE hardware may be required. An emulator can be used for testing the interface and non-Bluetooth portions of the application.

## Project Value

Urisis Android demonstrates experience with modern Android development, Jetpack Compose UI, multi-screen navigation, ViewModels, local persistence, Bluetooth permissions, BLE device workflows, and health-related application design.

## Repository

https://github.com/clstljr/Urisis_Android

## Future Improvements

- Add screenshots and a short demonstration video
- Document the BLE device protocol and supported hardware
- Add automated tests for the urinalysis workflow
- Improve error states for unavailable Bluetooth devices
- Document the local database schema
- Add a clear privacy and data-retention policy
