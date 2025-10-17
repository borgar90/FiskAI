# Model Training Guide for Fisk

This guide will help you train a fish classification model for the Fisk app.

## Prerequisites

- Python 3.8+
- TensorFlow 2.x
- A dataset of Norwegian fish images

## Step 1: Prepare Your Dataset

Organize your images in this structure:
```
dataset/
├── train/
│   ├── Torsk/
│   ├── Sei/
│   ├── Hyse/
│   ├── Laks/
│   ├── Ørret/
│   ├── Makrell/
│   ├── Sild/
│   ├── Rødspette/
│   ├── Kveite/
│   └── Abbor/
└── validation/
    ├── Torsk/
    ├── Sei/
    └── ...
```

## Step 2: Install Dependencies

```bash
pip install tensorflow tensorflow-datasets pillow numpy matplotlib
```

## Step 3: Training Script

Create `train_fish_model.py`:

```python
import tensorflow as tf
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout
from tensorflow.keras.models import Model
from tensorflow.keras.preprocessing.image import ImageDataGenerator
from tensorflow.keras.optimizers import Adam
from tensorflow.keras.callbacks import ModelCheckpoint, EarlyStopping

# Configuration
IMG_SIZE = 224
BATCH_SIZE = 32
EPOCHS = 50
NUM_CLASSES = 10
LEARNING_RATE = 0.0001

# Data augmentation
train_datagen = ImageDataGenerator(
    rescale=1./255,
    rotation_range=20,
    width_shift_range=0.2,
    height_shift_range=0.2,
    horizontal_flip=True,
    zoom_range=0.2,
    fill_mode='nearest'
)

val_datagen = ImageDataGenerator(rescale=1./255)

# Load data
train_generator = train_datagen.flow_from_directory(
    'dataset/train',
    target_size=(IMG_SIZE, IMG_SIZE),
    batch_size=BATCH_SIZE,
    class_mode='categorical'
)

validation_generator = val_datagen.flow_from_directory(
    'dataset/validation',
    target_size=(IMG_SIZE, IMG_SIZE),
    batch_size=BATCH_SIZE,
    class_mode='categorical'
)

# Build model with transfer learning
base_model = MobileNetV2(
    input_shape=(IMG_SIZE, IMG_SIZE, 3),
    include_top=False,
    weights='imagenet'
)

# Freeze base model
base_model.trainable = False

# Add custom head
x = base_model.output
x = GlobalAveragePooling2D()(x)
x = Dense(256, activation='relu')(x)
x = Dropout(0.5)(x)
x = Dense(128, activation='relu')(x)
x = Dropout(0.3)(x)
predictions = Dense(NUM_CLASSES, activation='softmax')(x)

model = Model(inputs=base_model.input, outputs=predictions)

# Compile model
model.compile(
    optimizer=Adam(learning_rate=LEARNING_RATE),
    loss='categorical_crossentropy',
    metrics=['accuracy', 'top_k_categorical_accuracy']
)

# Callbacks
checkpoint = ModelCheckpoint(
    'best_fish_model.h5',
    monitor='val_accuracy',
    save_best_only=True,
    mode='max',
    verbose=1
)

early_stop = EarlyStopping(
    monitor='val_loss',
    patience=10,
    restore_best_weights=True,
    verbose=1
)

# Train model
history = model.fit(
    train_generator,
    epochs=EPOCHS,
    validation_data=validation_generator,
    callbacks=[checkpoint, early_stop]
)

# Fine-tuning: Unfreeze some layers
base_model.trainable = True
for layer in base_model.layers[:-30]:
    layer.trainable = False

model.compile(
    optimizer=Adam(learning_rate=LEARNING_RATE/10),
    loss='categorical_crossentropy',
    metrics=['accuracy', 'top_k_categorical_accuracy']
)

# Continue training
history_fine = model.fit(
    train_generator,
    epochs=20,
    validation_data=validation_generator,
    callbacks=[checkpoint, early_stop]
)

# Convert to TensorFlow Lite
converter = tf.lite.TFLiteConverter.from_keras_model(model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_model = converter.convert()

# Save TFLite model
with open('fish_model.tflite', 'wb') as f:
    f.write(tflite_model)

print("Model training complete!")
print(f"TensorFlow Lite model saved as 'fish_model.tflite'")
print("Copy this file to app/src/main/assets/")
```

## Step 4: Train the Model

```bash
python train_fish_model.py
```

## Step 5: Deploy to App

1. Copy `fish_model.tflite` to `app/src/main/assets/`
2. Rebuild the Android app
3. Test with real fish images

## Data Collection Tips

1. **Collect diverse images**: Different angles, lighting, backgrounds
2. **Minimum images**: 100-500 per class
3. **Sources**:
   - Public fish databases
   - Wikimedia Commons
   - iNaturalist
   - Your own photos
4. **Data quality**: Clear, well-lit images work best

## Model Performance Tips

- Use data augmentation to prevent overfitting
- Monitor validation accuracy during training
- Consider using EfficientNet for better accuracy
- Test with real-world images before deploying

## Troubleshooting

- **Low accuracy**: Collect more training data
- **Overfitting**: Increase dropout or data augmentation
- **Slow inference**: Use quantization when converting to TFLite
- **Large model size**: Use MobileNetV2 or optimize with pruning

## Resources

- [TensorFlow Lite Guide](https://www.tensorflow.org/lite)
- [Transfer Learning Tutorial](https://www.tensorflow.org/tutorials/images/transfer_learning)
- [Fish Image Datasets](https://github.com/topics/fish-dataset)
