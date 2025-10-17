# Training Pipeline

This folder contains a simple training pipeline to create a TensorFlow Lite model for the Android app.

## Dataset Structure

Place your images in subfolders named by class under a single root folder, for example when training only salmon (Laks):

```
dataset/
└── Laks/
    ├── img001.jpg
    ├── img002.jpg
    └── ...
```

For multiple classes:

```
dataset/
├── Laks/
├── Torsk/
├── Sei/
└── ...
```

Recommended: at least 100 images per class, varied angles and lighting.

## Windows Setup (PowerShell)

```powershell
# Go to repo root
cd D:\dev\fisk

# Create venv
python -m venv .venv

# Activate venv
.\.venv\Scripts\Activate.ps1

# Install deps
pip install --upgrade pip
pip install -r training\requirements.txt

# Train on your dataset (update path accordingly)
python training\train.py --data_dir D:\path\to\dataset --output_dir D:\dev\fisk\training\outputs --epochs 12

# After training, copy model + labels into the Android app assets
copy training\outputs\fish_model.tflite app\src\main\assets\fish_model.tflite
copy training\outputs\labels.txt app\src\main\assets\labels.txt
```

Now rebuild and run the Android app. The classifier will read the labels and infer the number of classes dynamically.

## Notes

- The model uses MobileNetV2 transfer learning.
- The Android side normalizes images to [-1, 1], which is compatible with MobileNetV2 expectations when trained with default preprocessing. For best results, keep IMG_SIZE=224.
- Fine-tuning unfreezes the last N layers for better accuracy after initial training.
