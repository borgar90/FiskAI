import argparse
import json
import os
from pathlib import Path
from typing import List

import numpy as np
import tensorflow as tf
from tensorflow.keras.applications import MobileNetV2
from tensorflow.keras.layers import Dense, GlobalAveragePooling2D, Dropout
from tensorflow.keras.models import Model
from tensorflow.keras.optimizers import Adam


def find_classes(data_dir: Path) -> List[str]:
    classes = sorted([d.name for d in data_dir.iterdir() if d.is_dir()])
    if not classes:
        raise RuntimeError(f"No class folders found in {data_dir}")
    return classes


def build_datasets(data_root: Path, img_size: int, batch_size: int, val_split: float):
    seed = 1337
    train_ds = tf.keras.utils.image_dataset_from_directory(
        data_root,
        validation_split=val_split,
        subset="training",
        seed=seed,
        image_size=(img_size, img_size),
        batch_size=batch_size,
        label_mode="categorical",
    )
    val_ds = tf.keras.utils.image_dataset_from_directory(
        data_root,
        validation_split=val_split,
        subset="validation",
        seed=seed,
        image_size=(img_size, img_size),
        batch_size=batch_size,
        label_mode="categorical",
    )

    # Cache and prefetch for performance
    AUTOTUNE = tf.data.AUTOTUNE
    train_ds = train_ds.cache().shuffle(1000).prefetch(buffer_size=AUTOTUNE)
    val_ds = val_ds.cache().prefetch(buffer_size=AUTOTUNE)
    return train_ds, val_ds


def build_model(num_classes: int, img_size: int):
    base = MobileNetV2(input_shape=(img_size, img_size, 3), include_top=False, weights="imagenet")
    base.trainable = False

    x = base.output
    x = GlobalAveragePooling2D()(x)
    x = Dense(256, activation="relu")(x)
    x = Dropout(0.4)(x)
    outputs = Dense(num_classes, activation="softmax", name="predictions")(x)
    model = Model(inputs=base.input, outputs=outputs)
    return model, base


def to_tflite(model: tf.keras.Model, out_path: Path):
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    try:
        converter.target_spec.supported_ops = [
            tf.lite.OpsSet.TFLITE_BUILTINS_INT8,
            tf.lite.OpsSet.TFLITE_BUILTINS,
        ]
    except Exception:
        pass
    tflite_model = converter.convert()
    out_path.write_bytes(tflite_model)


def main():
    parser = argparse.ArgumentParser(description="Train fish classifier and export TFLite model")
    parser.add_argument("--data_dir", type=str, required=True, help="Path to dataset root. Expect class subfolders.")
    parser.add_argument("--output_dir", type=str, default="models", help="Directory to write outputs")
    parser.add_argument("--img_size", type=int, default=224)
    parser.add_argument("--batch_size", type=int, default=32)
    parser.add_argument("--epochs", type=int, default=10)
    parser.add_argument("--val_split", type=float, default=0.2)
    parser.add_argument("--fine_tune_layers", type=int, default=30, help="Unfreeze last N layers for fine-tuning")
    args = parser.parse_args()

    data_dir = Path(args.data_dir)
    output_dir = Path(args.output_dir)
    output_dir.mkdir(parents=True, exist_ok=True)

    class_names = find_classes(data_dir)
    num_classes = len(class_names)
    print(f"Found {num_classes} classes: {class_names}")

    # Save labels in deterministic order
    labels_txt = output_dir / "labels.txt"
    labels_txt.write_text("\n".join(class_names), encoding="utf-8")

    train_ds, val_ds = build_datasets(data_dir, args.img_size, args.batch_size, args.val_split)

    model, base = build_model(num_classes, args.img_size)
    model.compile(optimizer=Adam(learning_rate=1e-3), loss="categorical_crossentropy", metrics=["accuracy"]) 

    callbacks = [
        tf.keras.callbacks.EarlyStopping(monitor="val_loss", patience=5, restore_best_weights=True),
        tf.keras.callbacks.ModelCheckpoint(filepath=str(output_dir / "checkpoint.keras"), monitor="val_accuracy", save_best_only=True),
        tf.keras.callbacks.ReduceLROnPlateau(monitor="val_loss", factor=0.5, patience=2)
    ]

    history = model.fit(train_ds, validation_data=val_ds, epochs=args.epochs, callbacks=callbacks)

    # Fine-tune last N layers
    base.trainable = True
    for layer in base.layers[:-args.fine_tune_layers]:
        layer.trainable = False
    model.compile(optimizer=Adam(learning_rate=1e-4), loss="categorical_crossentropy", metrics=["accuracy"]) 
    model.fit(train_ds, validation_data=val_ds, epochs=max(3, args.epochs // 2), callbacks=callbacks)

    # Export to TFLite and Keras format
    tflite_path = output_dir / "fish_model.tflite"
    to_tflite(model, tflite_path)
    model.save(output_dir / "model.keras")

    print(f"Saved: {tflite_path}")
    print(f"Saved: {labels_txt}")
    print("Training complete.")


if __name__ == "__main__":
    main()
