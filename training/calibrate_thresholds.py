import argparse
import json
from pathlib import Path
import numpy as np
import tensorflow as tf


def load_model(model_path: Path):
    return tf.keras.models.load_model(model_path)


def load_dataset(dir_path: Path, img_size: int, batch_size: int):
    ds = tf.keras.utils.image_dataset_from_directory(
        dir_path,
        image_size=(img_size, img_size),
        batch_size=batch_size,
        shuffle=False,
        label_mode="categorical",
    )
    class_names = ds.class_names
    AUTOTUNE = tf.data.AUTOTUNE
    from tensorflow.keras.applications.mobilenet_v2 import preprocess_input

    def _preprocess(x, y):
        return preprocess_input(x), y

    ds = ds.map(_preprocess, num_parallel_calls=AUTOTUNE).prefetch(AUTOTUNE)
    return ds, class_names


def calibrate(model, ds, class_names, percentile: float = 0.8):
    # Collect confidences for correctly classified samples per class
    per_class_scores = {name: [] for name in class_names}
    for x, y in ds:
        preds = model.predict(x, verbose=0)
        if preds.shape[-1] == 1:
            # Single-class: skip calibration here
            continue
        y_true_idx = np.argmax(y.numpy(), axis=1)
        y_pred_idx = np.argmax(preds, axis=1)
        correct = y_true_idx == y_pred_idx
        for i, ok in enumerate(correct):
            if ok:
                cls = class_names[y_true_idx[i]]
                conf = float(np.max(preds[i]))
                per_class_scores[cls].append(conf)

    thresholds = {}
    for cls, scores in per_class_scores.items():
        if scores:
            scores_sorted = sorted(scores)
            k = max(0, int(len(scores_sorted) * percentile) - 1)
            thresholds[cls] = round(scores_sorted[k], 4)
        else:
            thresholds[cls] = 0.6  # default fallback
    return thresholds


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--val_dir", required=True)
    ap.add_argument("--model", required=False, default="training/outputs/model.keras")
    ap.add_argument("--img_size", type=int, default=224)
    ap.add_argument("--batch_size", type=int, default=32)
    ap.add_argument("--percentile", type=float, default=0.8, help="Percentile of correct confidences to use per class")
    ap.add_argument("--config", default="app/src/main/assets/config.json")
    args = ap.parse_args()

    model = load_model(Path(args.model))
    ds, class_names = load_dataset(Path(args.val_dir), args.img_size, args.batch_size)
    per_class = calibrate(model, ds, class_names, args.percentile)

    # Load existing config
    cfg_path = Path(args.config)
    cfg = json.loads(cfg_path.read_text(encoding="utf-8")) if cfg_path.exists() else {}
    cfg.setdefault("thresholds", {})
    # Set multiClass to min of per-class or default 0.6
    if per_class:
        cfg["thresholds"]["multiClass"] = float(min(per_class.values()))
    cfg["perClassThresholds"] = per_class

    cfg_path.write_text(json.dumps(cfg, ensure_ascii=False, indent=2), encoding="utf-8")
    print("Updated thresholds:", json.dumps(per_class, indent=2))


if __name__ == "__main__":
    main()
