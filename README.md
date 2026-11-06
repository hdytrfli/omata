<p align="center">
  <img src="icons/omata-512.png" width="120" alt="Omata icon">
</p>

<h1 align="center">Omata</h1>

<p align="center">A minimal camera OCR app for Android.</p>

<p align="center"><strong>1.0.0-beta.1</strong></p>

Omata reads text from whatever the camera is pointing at. Capture a photo, pick
the line you need, and copy it. Recognition happens on the device: the photo is
never saved and never leaves the phone.

## Camera

- Full-screen camera preview with a single shutter action
- Photos are captured into memory and discarded after recognition
- No storage permission and no files written to disk

## Text recognition

- On-device recognition with the bundled ML Kit Latin model
- Works offline and does not require Google Play Services
- Reads printed text in the captured frame

## Copying

- Results stack as pills over the live preview
- Tap a pill or its **Copy** action to copy that line
- **Copy all** copies the full block
- Dismiss the results and keep scanning

## Privacy

- Only the camera permission is requested
- Network permissions are removed from the final manifest, so the app cannot
  connect to anything
- No accounts, no analytics, no server

## Interface

- Material 3 interface built with Jetpack Compose
- Dark theme with an edge-to-edge camera view

## Requirements

- Android 9.0 or newer
- A back-facing camera
- arm64-v8a device for the current builds

## Current limitations

- The bundled model recognizes Latin script only
- Recognition runs on a captured photo, not on a continuous frame stream
- No torch or tap-to-focus controls

## Links

- [Omata on GitHub](https://github.com/hdytrfli/omata)
- [Releases](https://github.com/hdytrfli/omata/releases)
