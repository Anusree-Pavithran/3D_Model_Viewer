3D Model Viewer Android App
Overview

This project is a single-activity Android application built using Kotlin and XML Views. 
The app allows users to load and display multiple .glb 3D models simultaneously on the screen. 
Each model appears inside an independent draggable and resizable container with dedicated interaction controls.
The application was designed with performance as the primary focus and tested with multiple models loaded at the same time.

Technical Details

Language: Kotlin
Minimum SDK: 24
UI: XML Views
3D Library: SceneView (built on Google Filament)

I selected SceneView because it provides a simplified integration layer over Google's Filament rendering engine while still maintaining good rendering performance for Android devices.

The entire application runs inside a single Activity without fragments or additional screens.
The app bundles multiple .glb files locally inside the assets folder and allows users to dynamically add models to the screen.
Users can load and interact with multiple 3D models at the same time.
Each model appears inside a movable card which can be dragged anywhere on the screen.
Users can resize model containers using: pinch gestures and resize handle

Each model container includes:
Interact button
Close button

Normal Mode
Drag = move container
Pinch = resize container

Interaction Mode
Drag = rotate 3D model
Pinch = zoom 3D model

The two interaction modes remain completely separated.

Each model can be independently removed from the canvas.

Optimizations applied: (with the help of AI tools)

.glb assets are bundled locally (no network loading)
Small optimized GLB files are used
Minimal UI hierarchy
Single Activity architecture
SceneView lifecycle bound to Activity lifecycle
Removed models properly destroy SceneView instances
GPU resources cleaned during card removal
TranslationX/Y used for movement instead of repeated layout passes
Lazy model loading using post {}
Hardware accelerated rendering
Crash-safe model loading using runCatching


