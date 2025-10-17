# Quick Start Guide - Running Fisk App

## Prerequisites
- Windows 10/11
- 8GB RAM minimum
- 10GB free disk space

## Step-by-Step Instructions

### 1. Install Android Studio (Includes JDK)

1. Download Android Studio: https://developer.android.com/studio
2. Run the installer (`android-studio-2024.x.x.xx-windows.exe`)
3. Follow the setup wizard:
   - ✅ Install Android Studio
   - ✅ Install Android Virtual Device
   - ✅ Install Android SDK
   - ✅ **Android Studio includes JDK 17 - No separate download needed!**
4. Click "Finish" when complete

**Important**: You do NOT need to download JDK separately. Android Studio bundles everything you need.

### 2. Open the Fisk Project

1. Launch Android Studio
2. On welcome screen, click **"Open"**
3. Browse to: `d:\dev\fisk`
4. Click **"OK"**

### 3. Wait for Gradle Sync

- Android Studio will automatically start syncing
- You'll see progress at the bottom: "Gradle Build Running..."
- **First time takes 5-10 minutes** (downloading dependencies)
- If prompted to update Gradle or plugins, click "Update"

### 4. Set Up Your Android Device

#### Option A: Use Your Physical Phone (Recommended - Faster & Better)

1. **Enable Developer Mode on your phone:**
   - Go to **Settings → About Phone**
   - Find "Build Number" and tap it **7 times**
   - You'll see "You are now a developer!"

2. **Enable USB Debugging:**
   - Go to **Settings → System → Developer Options**
   - Toggle on **"USB Debugging"**

3. **Connect to PC:**
   - Connect your phone via USB cable
   - On your phone, accept the "Allow USB Debugging?" prompt
   - ✅ Check "Always allow from this computer"

4. **Verify Connection:**
   - In Android Studio, you should see your device in the device dropdown at the top

#### Option B: Use Android Emulator (Slower but no phone needed)

1. In Android Studio, click **Tools → Device Manager**
2. Click **"Create Device"**
3. Select **"Pixel 6"** → Click **"Next"**
4. Download a system image:
   - Click **"Download"** next to **"Android 14.0 (API 34)"**
   - Wait for download to complete
   - Click **"Next"** → **"Finish"**
5. Click the ▶ (play) button next to your emulator to start it

### 5. Run the App

1. **Select Device:**
   - At the top of Android Studio, select your device from the dropdown
   
2. **Click Run:**
   - Click the green ▶ **"Run"** button (or press `Shift + F10`)
   
3. **Wait for Build:**
   - First build takes 2-5 minutes
   - You'll see progress: "Building 'app'..."
   
4. **App Launches!**
   - The Fisk app will automatically install and open on your device
   - You'll see the welcome screen with the fish icon

### 6. Grant Camera Permission

- When you click "Start Kamera", the app will request camera permission
- Tap **"Allow"** to enable fish identification

## Troubleshooting

### "Could not find or load main class"
**Solution**: Android Studio's JDK is not configured
- File → Project Structure → SDK Location
- Set "JDK Location" to Android Studio's bundled JDK (usually `C:\Program Files\Android\Android Studio\jbr`)

### "SDK location not found"
**Solution**: Create `local.properties` file in project root:
```
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```
(Replace YOUR_USERNAME with your Windows username)

### Gradle sync fails
**Solution**: 
```powershell
cd d:\dev\fisk
.\gradlew clean
```
Then in Android Studio: File → Sync Project with Gradle Files

### "Emulator: Process finished with exit code -1"
**Solution**: Enable virtualization in BIOS or use a physical device

### App crashes immediately
**Solution**: 
1. Check Logcat (bottom of Android Studio) for error messages
2. The ML model file is missing (expected) - app will show "Could not identify fish"
3. Camera and UI will still work for testing

## What About the JDK?

**You don't need to do anything with JDK files!** 

Android Studio automatically:
- ✅ Includes JDK 17 (located in Android Studio's installation folder)
- ✅ Configures it for your project
- ✅ Uses it for building the app

If you already downloaded JDK separately, you can ignore it - Android Studio's bundled JDK is preferred.

## Testing Without ML Model

The app will work without the TensorFlow Lite model file:
- ✅ Camera functionality works
- ✅ UI and navigation work
- ✅ Fish database is accessible
- ⚠️ Classification will return empty results until you add `fish_model.tflite`

To add the ML model later, see `MODEL_TRAINING.md`

## Next Steps After Running

1. **Test Camera**: Click "Start Kamera" and test photo capture
2. **Explore UI**: Navigate through the screens
3. **Train Model**: Follow `MODEL_TRAINING.md` to add fish recognition
4. **Customize**: Modify colors, add more fish species, etc.

## Quick Commands Reference

Open PowerShell in `d:\dev\fisk`:

```powershell
# Build debug APK
.\gradlew assembleDebug

# Install on connected device
.\gradlew installDebug

# Clean build
.\gradlew clean

# Build and install
.\gradlew installDebug
```

APK will be in: `app\build\outputs\apk\debug\app-debug.apk`

## Still Need Help?

1. Check Android Studio's **"Build"** tab for errors
2. Check **"Logcat"** tab for runtime errors
3. See `DEVELOPMENT.md` for more detailed troubleshooting
4. The app structure is complete - main limitation is the missing ML model

---

**Summary**: Install Android Studio → Open project → Connect phone → Click Run ▶ → Done! 🎉
