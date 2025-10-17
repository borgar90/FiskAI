# Development Setup Guide

## Getting Started with Fisk Android App

This guide will help you set up the development environment for the Fisk fish identification app.

## System Requirements

- **Operating System**: Windows 10/11, macOS 10.14+, or Ubuntu 18.04+
- **RAM**: Minimum 8GB (16GB recommended)
- **Disk Space**: 10GB free space

## Step 1: Install Android Studio

1. Download Android Studio from: https://developer.android.com/studio
2. Install Android Studio with default settings
3. During setup, install the following:
   - Android SDK
   - Android SDK Platform
   - Android Virtual Device (AVD)

## Step 2: Install Required SDK Components

Open Android Studio and navigate to **Tools > SDK Manager**:

### SDK Platforms
- Android 14.0 (API 34) - Target SDK
- Android 7.0 (API 24) - Minimum SDK

### SDK Tools
- Android SDK Build-Tools 34.0.0
- Android Emulator
- Android SDK Platform-Tools
- Intel/AMD HAXM (for emulator acceleration)

## Step 3: Configure JDK

The project requires JDK 17:

1. Android Studio includes JDK - use the bundled version
2. Or install separately from: https://adoptium.net/

Set in Android Studio:
- **File > Project Structure > SDK Location > JDK Location**

## Step 4: Clone and Open Project

```bash
cd d:\dev
# Project already exists in fisk directory
```

Open in Android Studio:
1. Launch Android Studio
2. Click **Open**
3. Navigate to `d:\dev\fisk`
4. Click **OK**

## Step 5: Sync Gradle

1. Android Studio will prompt to sync Gradle
2. Click **Sync Now**
3. Wait for dependencies to download (may take 5-10 minutes first time)

## Step 6: Set Up Android Device or Emulator

### Option A: Physical Device (Recommended)

1. Enable **Developer Options** on your Android phone:
   - Go to Settings > About Phone
   - Tap "Build Number" 7 times
2. Enable **USB Debugging**:
   - Settings > Developer Options > USB Debugging
3. Connect phone via USB
4. Accept debugging authorization on phone

### Option B: Android Emulator

1. Open **Tools > Device Manager**
2. Click **Create Device**
3. Select a device (e.g., Pixel 6)
4. Download a system image (Android 13 or 14 recommended)
5. Finish setup and launch emulator

## Step 7: Build and Run

1. Select your device/emulator from device dropdown
2. Click the green **Run** button (or press Shift+F10)
3. Wait for build to complete
4. App should launch on device/emulator

## Project Structure Explained

```
fisk/
├── app/                          # Main application module
│   ├── src/
│   │   └── main/
│   │       ├── java/com/fisk/   # Kotlin source files
│   │       │   ├── MainActivity.kt        # Home screen
│   │       │   ├── CameraActivity.kt      # Camera UI
│   │       │   ├── ml/                    # ML inference code
│   │       │   ├── data/                  # Fish database
│   │       │   └── ui/                    # UI components
│   │       ├── res/             # Resources (layouts, strings, etc.)
│   │       └── assets/          # ML model and labels
│   └── build.gradle.kts         # Module build configuration
├── build.gradle.kts             # Project build configuration
├── settings.gradle.kts          # Project settings
└── gradle.properties            # Gradle properties
```

## Common Issues and Solutions

### Issue: Gradle sync fails

**Solution**: 
```bash
# Clean and rebuild
./gradlew clean
./gradlew build
```

### Issue: Cannot find Android SDK

**Solution**: Set SDK path in `local.properties`:
```
sdk.dir=C\:\\Users\\YourName\\AppData\\Local\\Android\\Sdk
```

### Issue: Emulator is slow

**Solution**:
- Enable hardware acceleration (HAXM/WHPX)
- Use x86 system images (faster than ARM)
- Increase emulator RAM in AVD settings

### Issue: App crashes on launch

**Solution**:
- Check Logcat for error messages
- Ensure camera permissions are granted
- Model file might be missing (expected until trained)

## Development Tips

### Viewing Logs

1. Open **Logcat** tab at bottom of Android Studio
2. Filter by package: `com.fisk`
3. Look for errors or your Log messages

### Debugging

1. Set breakpoints by clicking line numbers
2. Run in **Debug mode** (Shift+F9)
3. Use debugger to inspect variables

### Hot Reload

- Use **Apply Changes** (Ctrl+F10) for quick updates
- Faster than full rebuild for UI/logic changes

## Building Release APK

1. **Build > Generate Signed Bundle/APK**
2. Select **APK**
3. Create or use existing keystore
4. Select **release** build variant
5. APK will be in `app/build/outputs/apk/release/`

## Next Steps

1. **Add ML Model**: See `MODEL_TRAINING.md` for training guide
2. **Test Camera**: Grant camera permissions and test capture
3. **Customize UI**: Modify layouts in `res/layout/`
4. **Add Features**: Implement additional fish species or features

## Useful Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run tests
./gradlew test

# Clean build
./gradlew clean

# List all tasks
./gradlew tasks
```

## Resources

- [Android Developers](https://developer.android.com/)
- [Kotlin Documentation](https://kotlinlang.org/docs/)
- [CameraX Guide](https://developer.android.com/training/camerax)
- [TensorFlow Lite Android](https://www.tensorflow.org/lite/android)
- [Material Design 3](https://m3.material.io/)

## Getting Help

- Check Android Studio's **Build** tab for errors
- View **Logcat** for runtime errors
- Search [Stack Overflow](https://stackoverflow.com/questions/tagged/android)
- Check project README.md for overview

Happy coding! 🐟
