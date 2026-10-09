# Form Analyzer

An Android application for real-time squat technique analysis using
MediaPipe Pose Landmarker. Developed as part of an engineering thesis.

> **Work in progress.** Session history, summaries, voice feedback and the
> final user interface are still under development.

## Features (current state)

- Live pose tracking from the front camera (33 BlazePose landmarks).
- Repetition counting with a hysteresis-based state machine.
- Detection of three technique errors, evaluated once per repetition:
  - **Insufficient depth** (side view): minimum hip-knee-ankle angle above 90°.
  - **Knee valgus** (front view): drop of the frontal plane projection angle (FPPA)
    by at least 10° from standing, counted only when the knee moves toward
    the body's centerline.
  - **Back rounding** (side view): forward angular velocity of the
    shoulder-hip segment relative to the vertical axis above 100°/s.
- Framing check: analysis starts only once the head, torso, legs and feet
  have been in frame for 15 consecutive frames.
- Automatic side/front view detection based on hip width relative to leg length.
- Landmark smoothing with an exponential moving average (α = 0.5),
  chosen from measurements of noise, lag and depth bias.

## Project structure

- `app/src/main/java/.../squat/` – analysis engine (pure Kotlin, no Android
  dependencies, unit-tested on the JVM).
- `app/src/main/java/.../fragment/CameraFragment.kt` – camera screen,
  connects MediaPipe results with the analysis engine.
- `app/src/main/java/.../debug/` – development tools (landmark jitter logger).

## Code origin

The project is based on the official Google sample
[mediapipe-samples / pose_landmarker / android](https://github.com/google-ai-edge/mediapipe-samples/tree/main/examples/pose_landmarker/android)
(Apache License 2.0). Files originating from the sample retain their
original license headers.

## Building

- Requires **JDK 18**.
- The pose model is downloaded automatically during the build.
- Unit tests: `./gradlew :app:testDebugUnitTest`

For best results select the **GPU** delegate in the in-app settings panel
(about 25–29 analyzed frames per second vs. about 15 on CPU).
