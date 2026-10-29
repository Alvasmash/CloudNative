import { writeFileSync, copyFileSync, mkdirSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const api = new URL(process.env.API_BASE_URL || 'https://jfbxfcaajg.execute-api.us-east-1.amazonaws.com');
const site = new URL(process.env.PAGES_BASE_URL || 'https://alvasmash.github.io/CloudNative/');
const preview = process.env.PAGES_LOCAL_PREVIEW === '1';
for (const [name, url] of [['API_BASE_URL', api], ['PAGES_BASE_URL', site]]) {
  const loopback = ['localhost', '127.0.0.1', '[::1]', '0.0.0.0'].includes(url.hostname);
  if ((!['https:'].includes(url.protocol) && !(preview && loopback && url.protocol === 'http:'))
      || (!preview && loopback) || url.username || url.password || url.search || url.hash) {
    throw new Error(`${name} debe ser una URL HTTPS pública sin credenciales, parámetros ni fragmentos.`);
  }
}
site.pathname = site.pathname.replace(/\/?$/, '/');
const apiUrl = api.href.replace(/\/$/, '');
writeFileSync(path.join(root, 'src/environments/environment.pages.generated.ts'),
  `import { microsoftSettings } from './microsoft-settings';\nexport const environment = { production: true, apiUrl: ${JSON.stringify(apiUrl)}, useHashRouting: true, microsoft: microsoftSettings };\n`);
const result = spawnSync(process.execPath, [path.join(root, 'node_modules/@angular/cli/bin/ng.js'), 'build',
  '--configuration', 'pages', '--base-href', site.pathname, '--output-path', 'dist/pages'], {cwd: root, stdio: 'inherit'});
if (result.status !== 0) process.exit(result.status || 1);
const output = path.join(root, 'dist/pages/browser');
copyFileSync(path.join(output, 'index.html'), path.join(output, '404.html'));
copyFileSync(path.join(output, 'index.html'), path.join(output, 'redirect.html'));
mkdirSync(path.join(output, 'redirect'), {recursive: true});
copyFileSync(path.join(output, 'index.html'), path.join(output, 'redirect/index.html'));
writeFileSync(path.join(output, '.nojekyll'), '');
writeFileSync(path.join(output, 'deployment.json'), JSON.stringify({apiUrl, siteUrl: site.href,
  microsoftRedirectUri: new URL('redirect', site).href, routing: 'hash'}, null, 2));
console.log(`Pages preparado: ${site.href}\nRetorno Microsoft: ${new URL('redirect', site).href}`);
