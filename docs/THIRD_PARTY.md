# Third-party image viewer

PhotoSwipe 5.4.4 by Dmytro Semenov — MIT License.

- Upstream: https://github.com/dimsemenov/PhotoSwipe
- Documentation: https://photoswipe.com/
- Distribution source: https://registry.npmjs.org/photoswipe/-/photoswipe-5.4.4.tgz
- Runtime: locally bundled PhotoSwipe Core in `app/src/main/assets/photoswipe.umd.min.js`; the historical filename is retained, and the build emits an IIFE setting `window.PhotoSwipe`. CSS is unchanged upstream `dist/photoswipe.css`.
- ClassNote 1.4.4 gesture changes: axis threshold 10 → 6px; preserve the first movement instead of resetting the start point on drag recognition; double-tap window 300 → 220ms.
- Rebuild runtime: install fixed `esbuild@0.25.12`, then run `node scripts/build-photoswipe.cjs`. The APK builder uses the checked-in runtime and does not need npm or a network connection for PhotoSwipe.
- `vendor/photoswipe-5.4.4/src/js/gestures/gestures.js` contains the marked source modifications. Other vendored sources are unchanged.
- License: full MIT text in `app/src/main/assets/photoswipe-LICENSE.txt`, `docs/PHOTOSWIPE-LICENSE.txt`, and the vendored source directory.
- Readable local-fork sources: `vendor/photoswipe-5.4.4/src/`.
- App integration and gesture policy: `app/src/main/assets/photo-viewer.js`.

No CDN or network requests are required at runtime.
