<p align="center">
  <img src="icons/omata.svg" width="110" alt="Omata icon">
</p>

<h1 align="center">Omata</h1>

<p align="center">A minimal camera OCR app for Android.</p>

<p align="center"><strong>1.0.0-beta.1</strong></p>

Omata reads the text in front of the camera. Capture a photo, pick the line you
need, and copy it. Recognition happens on the device, so the photo is never
saved and never leaves the phone.

## Camera

- Full-screen camera preview with a single shutter action
- Captures into memory only, with no output file or storage permission
- The captured photo is discarded once recognition finishes
- Uses the back camera in portrait orientation

## Text recognition

- On-device recognition with the bundled ML Kit Latin model
- Works offline and does not require Google Play Services
- Returns the detected text line by line, in reading order
- Keeps the photo and the recognized text on the device

## Copying

- Detected lines appear as stacked pills over the live preview
- Tap a pill or its **Copy** action to copy that line
- **Copy all** copies every detected line at once
- A snackbar confirms each copy
- Dismiss the results and keep scanning; the next capture replaces them

## Privacy

- Only the camera permission is requested
- Network permissions are removed from the final manifest, so the app cannot
  reach the internet
- No accounts, no analytics, no server, and no files written to storage

## Interface

- Material 3 interface built with Jetpack Compose
- Dark theme with an edge-to-edge camera view
- Results float over the preview so the camera stays live
- Insets follow the status and navigation bars

## Requirements

- Android 9.0 or newer
- A back-facing camera
- arm64-v8a device for the current builds

## Current limitations

- The bundled model recognizes Latin script only
- Recognition runs on a captured photo rather than a continuous frame stream
- No torch, zoom, or tap-to-focus controls

## Links

- [Omata on GitHub](https://github.com/hdytrfli/omata)
- [Releases](https://github.com/hdytrfli/omata/releases)
