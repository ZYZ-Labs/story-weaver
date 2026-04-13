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

  function setTextareaValue(value) {
    const textareas = Array.from(document.querySelectorAll('textarea')).filter((node) => node.offsetParent !== null);
    const textarea = textareas.length >= 2 ? textareas[textareas.length - 2] : textareas[0];
    if (!textarea) {
      throw new Error('未找到输入框 textarea');
    }
    textarea.focus();
    textarea.value = value;
    textarea.dispatchEvent(new Event('input', { bubbles: true }));
    textarea.dispatchEvent(new Event('change', { bubbles: true }));
    return textarea;
  }

  async function clickByText(text, timeoutMs) {
    const node = await waitFor(() => findEnabledClickableByText(text), timeoutMs, `按钮 ${text}`);
    node.click();
    return node;
  }

  function summaryDraftReady() {
    const editors = Array.from(document.querySelectorAll('textarea')).filter((node) => node.offsetParent !== null);
    const editor = editors[editors.length - 1];
    if (!editor) {
      return false;
    }
    return normalizedText(editor.value).length > 20;
  }

  function assistantMessageCount() {
    return Array.from(document.querySelectorAll('.summary-chat-message__role'))
      .map((node) => normalizedText(node.textContent))
      .filter((text) => text === 'AI 助手').length;
  }

  function currentBodyText() {
    return normalizedText(document.body.innerText || document.body.textContent || '');
  }

  async function runCreateScenario(label, promptText) {
    await waitFor(() => findTextNode(label), 15000, `页面标题 ${label}`);
    pushCheck('page-loaded', true, label);
    pushCheck(
      'top-level-ordinary-mode-lite',
      Boolean(findClickableExactText('切到专家模式')) && !Boolean(findClickableExactText('专家模式')),
      '页面顶层默认应降级为普通模式提示，而不是直接展示模式分段切换器',
    );

    await clickByText(`说想法新增${label.replace('管理', '')}`, 15000);
    await waitFor(() => findTextNode('对话采集'), 10000, '对话采集');
    await waitFor(() => findTextNode('摘要草稿'), 10000, '摘要草稿');
    pushCheck('dialog-opened', true, '摘要工作流已打开');

    const bodyText = currentBodyText();
    pushCheck('ordinary-mode-system-terms-hidden', !bodyText.includes('REFINE') && !bodyText.includes('UPDATE') && !bodyText.includes('ENRICH'), '普通模式不应直接暴露结构意图英文枚举');

    const assistantCountBefore = assistantMessageCount();
    setTextareaValue(promptText);
    await clickByText('让 AI 继续整理', 5000);
    await waitFor(
      () => assistantMessageCount() > assistantCountBefore || summaryDraftReady(),
      25000,
      'AI 首轮回应',
    );
    pushCheck('chat-responded', true, '普通模式已返回追问或摘要草稿');

    await clickByText('看看整理结果', 10000);
    await waitFor(() => findTextNode('变化预览'), 15000, '变化预览');
    pushCheck('preview-opened', true, '已看到变化预览');
  }

  async function runChapterEditScenario() {
    await waitFor(() => findTextNode('章节管理'), 15000, '章节管理');
    pushCheck('page-loaded', true, '章节管理');
    pushCheck(
      'top-level-ordinary-mode-lite',
      Boolean(findClickableExactText('切到专家模式')) && !Boolean(findClickableExactText('专家模式')),
      '页面顶层默认应降级为普通模式提示，而不是直接展示模式分段切换器',
    );

    await waitFor(() => findClickableByText('摘要优先编辑') || findClickableByText('摘要优先'), 15000, '摘要优先入口');
    const entry = findClickableByText('摘要优先编辑') || findClickableByText('摘要优先');
    entry.click();
    await waitFor(() => findTextNode('对话采集'), 10000, '对话采集');
    pushCheck('dialog-opened', true, '章节摘要编辑弹层已打开');

    const assistantCountBefore = assistantMessageCount();
    setTextareaValue('我想让这章摘要更明确一点，先让读者看到林沉舟现在的现实状态，再把邀请函作为触发点抛出来，最后停在他决定赴约。');
    await clickByText('让 AI 继续整理', 5000);
    await waitFor(
      () => assistantMessageCount() > assistantCountBefore || summaryDraftReady(),
      25000,
      '章节摘要对话结果',
    );
    pushCheck('chat-responded', true, '章节普通模式已返回草稿');

    await clickByText('看看整理结果', 10000);
    await waitFor(() => findTextNode('变化预览'), 15000, '变化预览');
    pushCheck('preview-opened', true, '章节编辑可进入预览');
  }

  async function runScenario() {
    try {
      await waitFor(() => document.body, 5000, 'document.body');
      await sleep(2500);
      if (scenario === 'characters-create') {
        await runCreateScenario('人物管理', '我想要一个很油滑的经纪人，表面圆滑，其实特别会算计，和主角以前有合作。');
      } else if (scenario === 'world-settings-create') {
        await runCreateScenario('世界观管理', '这个世界里，职业资格不是考试决定的，而是靠一种叫回声签约的仪式。签约成功后会得到能力，失败后会留下记忆缺口。');
      } else if (scenario === 'chapters-create') {
        await runCreateScenario('章节管理', '我想写一章主角和这个经纪人重新见面的戏。先写主角本来不想理他，经纪人又装得很热情，最后停在经纪人抛出一个让主角动摇的条件。');
      } else if (scenario === 'chapters-edit') {
        await runChapterEditScenario();
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
