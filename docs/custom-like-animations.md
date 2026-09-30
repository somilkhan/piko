# Custom Piko Like Animations

Piko's Instagram 439.0.0.37.89 like-animation patch supports user-imported animation packages.

## Package format

The file extension is `.pikoanim`, but the file is a ZIP archive.

Required root file:

    manifest.json

Example manifest:

    {
      "format": 1,
      "id": "my-heart",
      "name": "My Heart",
      "animation": "animation.gif"
    }

The animation may be a GIF, animated WebP, or PNG inside the package. The package is data-only; executable code is never loaded.

## Limits

- Package: 20 MiB maximum
- Individual animation asset: 12 MiB maximum
- Manifest: 64 KiB maximum
- IDs: ASCII letters, digits, `.`, `_`, `-`
- Paths must remain inside the package directory

Imported packages are copied into Instagram's private app storage and registered in Piko settings. Selecting an imported entry uses the custom drawable path in Instagram's LikeActionView; if decoding fails, Piko falls back to the normal built-in animation.

## Example

Create `manifest.json` and `animation.gif`, ZIP them, and rename the archive to `my-heart.pikoanim`.

Then open: Piko Settings → Misc → Change like animation → Import custom animation.