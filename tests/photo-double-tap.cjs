const { chromium } = require('playwright');
const assert = require('node:assert/strict');
const http = require('node:http');
const fs = require('node:fs');
const path = require('node:path');
const base = path.resolve(__dirname, '../app/src/main/assets');

(async () => {
  const server = http.createServer((req, res) => {
    fs.readFile(path.join(base, req.url === '/' ? 'index.html' : req.url.split('?')[0]), (error, data) => {
      if (error) res.writeHead(404).end();
      else {
        res.setHeader('Content-Type', req.url.endsWith('.js') ? 'application/javascript' : req.url.endsWith('.css') ? 'text/css' : 'text/html');
        res.end(data);
      }
    });
  }).listen(0, '127.0.0.1');
  const browser = await chromium.launch({ headless: true, executablePath: process.env.CHROME_BIN, args: ['--no-sandbox'] });
  let failed = 0;
  async function run(name, action) {
    const page = await browser.newPage({ viewport: { width: 390, height: 844 }, hasTouch: true, isMobile: true });
    page.setDefaultTimeout(3000);
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    try {
      await page.goto(`http://127.0.0.1:${server.address().port}`);
      await page.evaluate(() => {
        const canvas = document.createElement('canvas');
        canvas.width = 800; canvas.height = 450;
        canvas.getContext('2d').fillRect(0, 0, 800, 450);
        const image = canvas.toDataURL();
        localStorage.setItem('media-double.png', image);
        window.openPhotos = () => PhotoViewer.open([{ name: 'double.png', label: 'Photo', msrc: image, width: 800, height: 450 }]);
        openPhotos();
      });
      await page.waitForFunction(() => document.querySelector('#overlay img[alt="Photo"]')?.naturalWidth > 0);
      await action(page);
      assert.deepEqual(errors, []);
      console.log('PASS', name);
    } catch (error) {
      failed++;
      console.error('FAIL', name, error.message);
    } finally { await page.close(); }
  }
  try {
    await run('300ms 间隔双击照片放大，再次双击还原', async page => {
      const photo = page.locator('#overlay img[alt="Photo"]');
      const initial = await photo.boundingBox();
      for (const zoomIn of [true, false]) {
        await page.touchscreen.tap(195, 422);
        await page.waitForTimeout(300);
        await page.touchscreen.tap(195, 422);
        await page.waitForTimeout(250);
        assert.ok(await page.locator('#overlay').isVisible(), '双击不能触发单击关闭');
        const bounds = await photo.boundingBox();
        assert.ok(zoomIn ? bounds.width > initial.width * 2 : Math.abs(bounds.width - initial.width) < 2);
      }
      assert.equal(await page.evaluate(() => visualViewport.scale), 1);
    });
    await run('鼠标双击照片放大', async page => {
      const photo = page.locator('#overlay img[alt="Photo"]');
      const initial = await photo.boundingBox();
      await page.mouse.click(195, 422);
      await page.waitForTimeout(180);
      await page.mouse.click(195, 422);
      await page.waitForTimeout(250);
      assert.ok(await page.locator('#overlay').isVisible(), '第一次鼠标点击不能提前关闭');
      assert.ok((await photo.boundingBox()).width > initial.width * 2);
    });
    await run('单击照片仍能退出', async page => {
      await page.touchscreen.tap(195, 422);
      await page.waitForFunction(() => document.querySelector('#overlay').hidden);
    });
    await run('空白区域保持立即退出', async page => {
      await page.touchscreen.tap(195, 40);
      await page.waitForTimeout(50);
      assert.ok(await page.evaluate(() => document.querySelector('#overlay').hidden));
    });
  } finally { await browser.close(); server.close(); }
  if (failed) process.exitCode = 1;
})().catch(error => { console.error(error); process.exitCode = 1; });
