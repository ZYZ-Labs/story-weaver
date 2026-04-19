import { execFileSync } from 'node:child_process';
import { mkdtempSync, rmSync } from 'node:fs';
import { tmpdir } from 'node:os';
import { join } from 'node:path';

const extensionDir = join(process.cwd(), 'tmp/browser-smoke/extension');
const baseUrl = 'https://home.silvericekey.fun:41202';
const scenarios = [
  { id: 'workbench-main', path: '/workbench' },
  { id: 'state-center-main', path: '/state-center' },
  { id: 'generation-center-main', path: '/generation-center' },
  { id: 'chapters-secondary-entry', path: '/chapters' },
];

function extractReport(dom) {
  const marker = 'id="codex-browser-report"';
  const idx = dom.indexOf(marker);
  if (idx < 0) {
    return null;
  }
  const start = dom.indexOf('>', idx);
  const end = dom.indexOf('</pre>', start);
  if (start < 0 || end < 0) {
    return null;
  }
  const raw = dom.slice(start + 1, end)
    .replaceAll('&quot;', '"')
    .replaceAll('&amp;', '&')
    .replaceAll('&lt;', '<')
    .replaceAll('&gt;', '>');
  return JSON.parse(raw);
}

const results = [];

for (const scenario of scenarios) {
  const userDataDir = mkdtempSync(join(tmpdir(), `story-weaver-browser-${scenario.id}-`));
  const url = `${baseUrl}${scenario.path}?codexScenario=${scenario.id}`;
  try {
    const dom = execFileSync(
      '/usr/bin/google-chrome',
      [
        '--headless=new',
        '--disable-gpu',
        '--no-sandbox',
        `--user-data-dir=${userDataDir}`,
        `--load-extension=${extensionDir}`,
        '--virtual-time-budget=30000',
        '--dump-dom',
        url,
      ],
      { encoding: 'utf8', maxBuffer: 10 * 1024 * 1024 },
    );
    results.push(extractReport(dom) || { scenario: scenario.id, ok: false, error: '未找到浏览器验收报告节点', checks: [] });
  } catch (error) {
    results.push({
      scenario: scenario.id,
      ok: false,
      error: error instanceof Error ? error.message : String(error),
      checks: [],
    });
  } finally {
    rmSync(userDataDir, { recursive: true, force: true });
  }
}

console.log(JSON.stringify(results, null, 2));
