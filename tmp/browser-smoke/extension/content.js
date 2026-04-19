(function () {
  const TOKEN = 'eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiJhZG1pbiIsInVzZXJJZCI6MSwiaWF0IjoxNzc2MDg3MzI4LCJleHAiOjE3NzYxNzM3Mjh9.AthyWkxE8OK9n7b-c_GSrmU7UwgIbUHy_Mnvw0AK53M';
  const USER = {
    id: 1,
    username: 'admin',
    roleCode: 'admin',
    displayName: 'admin',
  };
  const PROJECT_ID = 28;

  localStorage.setItem('story-weaver:token', JSON.stringify(TOKEN));
  localStorage.setItem('story-weaver:user', JSON.stringify(USER));
  localStorage.setItem('story-weaver:project-id', JSON.stringify(PROJECT_ID));

  const params = new URLSearchParams(location.search);
  const scenario = params.get('codexScenario');
  if (!scenario) {
    return;
  }

  const report = {
    scenario,
    page: location.pathname,
    checks: [],
    ok: false,
    error: null,
  };

  function pushCheck(name, passed, detail) {
    report.checks.push({ name, passed, detail: detail || '' });
  }

  function publishReport() {
    let node = document.getElementById('codex-browser-report');
    if (!node) {
      node = document.createElement('pre');
      node.id = 'codex-browser-report';
      node.style.position = 'fixed';
      node.style.left = '0';
      node.style.bottom = '0';
      node.style.zIndex = '999999';
      node.style.maxWidth = '100vw';
      node.style.maxHeight = '50vh';
      node.style.overflow = 'auto';
      node.style.background = 'rgba(0,0,0,0.85)';
      node.style.color = '#fff';
      node.style.padding = '12px';
      node.style.fontSize = '12px';
      document.documentElement.appendChild(node);
    }
    node.textContent = JSON.stringify(report, null, 2);
  }

  function sleep(ms) {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  function normalizedText(value) {
    return (value || '').replace(/\s+/g, ' ').trim();
  }

  function queryAllClickable() {
    return Array.from(document.querySelectorAll('button, [role="button"], .v-btn'));
  }

  function findClickableByText(text) {
    return queryAllClickable().find((node) => normalizedText(node.textContent).includes(text));
  }

  function findClickableExactText(text) {
    return queryAllClickable().find((node) => normalizedText(node.textContent) === text);
  }

  function findEnabledClickableByText(text) {
    const node = findClickableByText(text);
    if (!node) {
      return null;
    }
    if (node.disabled || node.getAttribute('aria-disabled') === 'true') {
      return null;
    }
    return node;
  }

  function findTextNode(text) {
    return Array.from(document.querySelectorAll('body *')).find((node) =>
      normalizedText(node.textContent).includes(text),
    );
  }

  async function waitFor(fn, timeoutMs, label) {
    const start = Date.now();
    while (Date.now() - start < timeoutMs) {
      const value = fn();
      if (value) {
        return value;
      }
      await sleep(200);
    }
    throw new Error(`等待超时: ${label}`);
  }

  async function clickByText(text, timeoutMs) {
    const node = await waitFor(() => findEnabledClickableByText(text), timeoutMs, `按钮 ${text}`);
    node.click();
    return node;
  }

  function currentBodyText() {
    return normalizedText(document.body.innerText || document.body.textContent || '');
  }

  async function runWorkbenchScenario() {
    await waitFor(() => findTextNode('创作台'), 15000, '创作台');
    await waitFor(() => findTextNode('当前项目简报'), 15000, '当前项目简报');
    await waitFor(() => findTextNode('下一步动作'), 15000, '下一步动作');
    await waitFor(() => findTextNode('章节骨架预览'), 15000, '章节骨架预览');
    await waitFor(() => findTextNode('章节执行状态'), 15000, '章节执行状态');
    pushCheck('page-loaded', true, '创作台已加载');
    pushCheck('cta-visible', Boolean(findClickableByText('说想法新增章节')), '创作台应暴露说想法新增章节');
    pushCheck('nav-groups-visible', currentBodyText().includes('故事台') && currentBodyText().includes('状态台') && currentBodyText().includes('生成台'), '新导航分组应可见');
  }

  async function runStateCenterScenario() {
    await waitFor(() => findTextNode('状态台'), 15000, '状态台');
    await waitFor(() => findTextNode('章节状态'), 15000, '章节状态');
    await waitFor(() => findTextNode('读者揭晓与 POV 状态'), 15000, '读者揭晓与 POV 状态');
    pushCheck('page-loaded', true, '状态台已加载');
    pushCheck('state-metrics-visible', currentBodyText().includes('未解环') && currentBodyText().includes('读者已知'), '状态台指标应可见');
  }

  async function runGenerationCenterScenario() {
    await waitFor(() => findTextNode('生成台'), 15000, '生成台');
    await waitFor(() => findTextNode('多 Session 编排预览'), 15000, '多 Session 编排预览');
    await waitFor(() => findTextNode('写手 Brief 与章节审校'), 15000, '写手 Brief 与章节审校');
    pushCheck('page-loaded', true, '生成台已加载');
    pushCheck('generation-metrics-visible', currentBodyText().includes('候选数') && currentBodyText().includes('骨架镜头'), '生成台指标应可见');
  }

  async function runChaptersSecondaryEntryScenario() {
    await waitFor(() => findTextNode('章节管理'), 15000, '章节管理');
    pushCheck('page-loaded', true, '章节页仍可访问');
    pushCheck('secondary-entry', currentBodyText().includes('故事台') || Boolean(findTextNode('故事台')), '章节页应作为故事台二级入口存在');
    pushCheck('summary-first-entry', Boolean(findClickableByText('说想法新增章节') || findClickableByText('摘要新增章节')), '章节页仍应保留摘要优先新增入口');
  }

  async function runScenario() {
    try {
      await waitFor(() => document.body, 5000, 'document.body');
      await sleep(2500);
      if (scenario === 'workbench-main') {
        await runWorkbenchScenario();
      } else if (scenario === 'state-center-main') {
        await runStateCenterScenario();
      } else if (scenario === 'generation-center-main') {
        await runGenerationCenterScenario();
      } else if (scenario === 'chapters-secondary-entry') {
        await runChaptersSecondaryEntryScenario();
      } else {
        throw new Error(`未知场景: ${scenario}`);
      }
      report.ok = report.checks.every((item) => item.passed);
    } catch (error) {
      report.ok = false;
      report.error = error instanceof Error ? error.message : String(error);
    } finally {
      publishReport();
    }
  }

  window.addEventListener('load', () => {
    runScenario().catch((error) => {
      report.ok = false;
      report.error = error instanceof Error ? error.message : String(error);
      publishReport();
    });
  });
})();
