# Pixel Studio

Pixel Studio is an integrated Life Minder utility for turning source images into editable pixel sprites and animation sheets. Open it from **Menu → Pixel Studio**.

## Workflow

1. Import an image or create a blank sprite.
2. In **Source**, choose the output dimensions, palette size, fit mode, resizing mode, background removal, and dithering.
3. Use **Draw** for pixel-level cleanup and outlines.
4. Use **Palette** for palette swaps or hue, saturation, brightness, and contrast variants.
5. Use **Transform** for flips, rotations, and an eight-direction frame set.
6. Use **Frames** and **Layers** for animation, onion skinning, timing, and compositing.
7. Use **Export** for a transparent PNG, a scaled nearest-neighbor PNG, a sprite sheet, or a portable editable project.

Projects autosave to private application storage and are included in Life Minder's full local backup and restore.

## Smart resizing

`SMART` examines the source and the direction of the size change:

- Reductions use area sampling to retain representative detail, then snap back to the existing palette when the source already has a compact pixel-art palette.
- Enlargements of palette-limited artwork use nearest-neighbor expansion to preserve hard pixel edges.
- Enlargements of continuous-tone source images use bilinear sampling before quantization.

The explicit `NEAREST`, `AREA`, and `BILINEAR` modes remain available when an artist wants predictable manual control.

## Pixelification

Pixelification combines fitting or centered cropping, smart resizing, alpha cleanup, deterministic color clustering, and optional Floyd–Steinberg error diffusion. Connected-corner background removal is an intentionally local, offline tool: it removes similarly colored regions connected to image corners without uploading the source image.

## Project limits

- Canvas dimensions are limited to 512×512 pixels to keep touch editing and autosave responsive.
- Imported images are decoded at a maximum dimension of 1024 pixels before processing.
- Direction generation performs geometric rotations. It does not invent unseen sides of a subject; generated frames are intended as editable starting points.
