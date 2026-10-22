// npm install --no-save esbuild@0.25.12; node scripts/build-photoswipe.cjs
// The packaged runtime is built from the readable, MIT-licensed local fork.
const path=require('node:path'),esbuild=require('esbuild');
if(require('esbuild/package.json').version!=='0.25.12')throw Error('Use esbuild 0.25.12');
esbuild.buildSync({stdin:{contents:"import PhotoSwipe from './vendor/photoswipe-5.4.4/src/js/photoswipe.js'; window.PhotoSwipe=PhotoSwipe;",resolveDir:path.resolve(__dirname,'..')},bundle:true,minify:true,format:'iife',target:['chrome80'],outfile:path.resolve(__dirname,'../app/src/main/assets/photoswipe.umd.min.js'),banner:{js:'/*! PhotoSwipe 5.4.4, Dmytro Semenov, MIT. ClassNote gesture patch; see photoswipe-LICENSE.txt. */'}});
