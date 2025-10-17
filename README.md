# FiskAI - Norwegian Fish Identifier 🐟

An Android application that identifies fish species in Norwegian waters using your phone's camera and machine learning.

## Features

- 📷 **Real-time Camera Detection**: Use your phone's camera to capture fish images
- 🤖 **AI-Powered Identification**: TensorFlow Lite model for fish species classification
- 🇳🇴 **Norwegian Waters Focus**: Database of common fish species found in Norwegian waters
- 📊 **Detailed Information**: View species details, characteristics, and habitat information
- 🎯 **Confidence Scoring**: See how confident the AI is about each identification

## Tech Stack

- **Language**: Kotlin
- **ML Framework**: TensorFlow Lite
- **Camera**: CameraX API
- **UI**: Material Design 3
- **Min SDK**: 24 (Android 7.0)
- **Target SDK**: 34 (Android 14)

## Norwegian Fish Species Supported

The app can identify common species including:
- Torsk (Atlantic Cod)
- Sei (Saithe/Pollock)
- Hyse (Haddock)
- Ørret (Trout)
- Laks (Salmon)
- Makrell (Mackerel)
- Sild (Herring)
- And more...

## Project Structure

```
fisk/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/fisk/
│   │   │   │   ├── MainActivity.kt
│   │   │   │   ├── CameraActivity.kt
│   │   │   │   ├── ml/
│   │   │   │   │   ├── FishClassifier.kt
│   │   │   │   │   └── ImageProcessor.kt
│   │   │   │   ├── data/
│   │   │   │   │   ├── FishDatabase.kt
│   │   │   │   │   └── FishSpecies.kt
│   │   │   │   └── ui/
│   │   │   │       ├── ResultActivity.kt
│   │   │   │       └── adapters/
│   │   │   ├── res/
│   │   │   │   ├── layout/
│   │   │   │   ├── values/
│   │   │   │   └── xml/
│   │   │   ├── assets/
│   │   │   │   ├── fish_model.tflite
│   │   │   │   └── labels.txt
│   │   │   └── AndroidManifest.xml
│   │   └── build.gradle.kts
│   └── build.gradle.kts
├── gradle/
└── settings.gradle.kts
```

## Setup Instructions

### Prerequisites

1. **Android Studio**: Download from [developer.android.com](https://developer.android.com/studio)
2. **JDK**: Version 17 or higher
3. **Android SDK**: API Level 24+

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/fisk.git
   cd fisk
   ```

2. Open the project in Android Studio

3. Sync Gradle files

4. Connect an Android device or start an emulator

5. Run the app (Shift + F10)

### Permissions Required

- **Camera**: For capturing fish images
- **Storage**: For saving and accessing photos (optional)

## Building the App

### Development Build
```bash
./gradlew assembleDebug
```

### Release Build
```bash
./gradlew assembleRelease
```

## ML Model Training (Optional)

To train your own fish classification model:

1. Collect a dataset of Norwegian fish images
2. Use TensorFlow/Keras to train a classification model
3. Convert to TensorFlow Lite format
4. Replace `fish_model.tflite` in assets folder

Quick path using the built-in training script (Windows):

1. Prepare a folder like `D:\data\fish\dataset\Laks\...` (or multiple class folders)
2. Open PowerShell and run:

```powershell
cd D:\dev\fisk
python -m venv .venv
.\.venv\Scripts\Activate.ps1
pip install -r training\requirements.txt
python training\train.py --data_dir D:\data\fish\dataset --output_dir training\outputs --epochs 12
copy training\outputs\fish_model.tflite app\src\main\assets\fish_model.tflite
copy training\outputs\labels.txt app\src\main\assets\labels.txt
```

## Usage

1. Launch the app
2. Point your camera at a fish
3. Tap the capture button
4. View identification results with confidence scores
5. Explore detailed species information

## Roadmap

- [ ] Offline mode with local database
- [ ] Fish size estimation
- [ ] Location-based species suggestions
- [ ] Fishing regulations and seasons
- [ ] Community photo sharing
- [ ] Multi-language support (Norwegian Bokmål, Nynorsk, English)

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## License

MIT License - See LICENSE file for details

## Acknowledgments

- Norwegian fisheries data
- TensorFlow Lite team
- CameraX library
- Material Design components

---

**Note**: This app is for educational and recreational purposes. Always follow local fishing regulations and conservation guidelines.
