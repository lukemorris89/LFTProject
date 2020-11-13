# android-camera-r-and-d

This Android Canmera R&D App is designed to demonstrate the features and functionality of Android's CameraX API with regards to previewing, analysing and capturing images of lateral flow tests.

## Getting Started

This repo contains an Android Studio project, which can be imported into Android Studio and then built and run on Android Devices or emulators.

The app will run on devices that meet these requirements:
- Minimum OS of Android v5.0 (Lollipop) or later (Minimum SDK: 21)
- Handset with rear camera
- HDPI or greater screen resolution
- Google Play store available


## Features

The app features:
- User can preview an image from the camera viewfinder and perform live analysis on an incoming stream of image frames
- Capture a photograph when the image in the preview meets the required criteria/matches the correct image analysis label (currently set to a hand for testing purposes)
- View the results of the image classification analysis of the captured photograph and retake the photograph if required
- Choose an image from the device's gallery and perform image classification analysis on it


## Dependencies

The following libraries are utilised in this project:
- ***AndroidX Navigation Components*** - navigation control and navigation graph
- ***AndroidX Lifecycle Components*** - viewmodels, livedata and lifecycle components
- ***Android Material*** - material design layout components
- ***Android CameraX*** - hardware camera image preview and capture
- ***Google MLKit*** - image classification

## Code Style

Code should be formatted according to the [Google Android Kotlin Style Guide](https://developer.android.com/kotlin/style-guide).
You can enable checking against the style guide in Android Studio by following the steps detailed [here](https://kotlinlang.org/docs/reference/coding-conventions.html)
