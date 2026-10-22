# ClassNote gesture patches

Base: PhotoSwipe 5.4.4, MIT, Dmytro Semenov.

Only `src/js/gestures/gestures.js` is modified: 6px axis threshold; retain the first movement in prevP1/prevP2; 350ms double-tap recognition; recognize mouse double-clicks on photos while preserving single-click actions.

Run `npm install --no-save esbuild@0.25.12` in the ClassNote root, then `node scripts/build-photoswipe.cjs`. This rebuilds the packaged runtime from these sources. Full license is in LICENSE. Other sources and CSS remain upstream.
