document.addEventListener('DOMContentLoaded', function() {
    const mdInput = document.getElementById('mdInput');
    const mdPreview = document.getElementById('mdPreview');
    const mdViewMode = document.getElementById('mdViewMode');
    const mdWorkspace = document.getElementById('mdWorkspace');
    const editorBox = document.getElementById('editorBox');
    const previewBox = document.getElementById('previewBox');
    const btnSample = document.getElementById('btnSample');
    const btnPaste = document.getElementById('btnPaste');
    const btnClear = document.getElementById('btnClear');
    const btnCopyHtml = document.getElementById('btnCopyHtml');
    const btnDownloadHtml = document.getElementById('btnDownloadHtml');

    const CACHE_MD = 'tool.markdown.input';
    const CACHE_VIEW = 'tool.markdown.view';
    const getLocal = (k) => (window.btf && window.btf.optLocal) ? window.btf.optLocal.get(k) : localStorage.getItem(k);
    const setLocal = (k, v, ttl = 365) => {
        if (window.btf && window.btf.optLocal) window.btf.optLocal.set(k, v, ttl);
        else localStorage.setItem(k, v);
    };

    const savedMd = getLocal(CACHE_MD);
    if (savedMd) mdInput.value = typeof savedMd === 'string' ? savedMd : savedMd;
    const savedView = getLocal(CACHE_VIEW);
    if (savedView && (savedView === 'split' || savedView === 'preview' || savedView === 'edit')) {
        mdViewMode.value = savedView;
    }

    const render = () => {
        const src = mdInput.value || '';
        let html = '';
        try {
            html = marked.parse(src, { gfm: true, breaks: true });
        } catch (e) {
            html = '<pre style="color:#dc3545">Markdown 解析错误</pre>';
        }
        mdPreview.innerHTML = DOMPurify.sanitize(html);
    };

    const applyView = () => {
        const mode = mdViewMode.value;
        if (mode === 'preview') {
            editorBox.classList.add('hidden');
            previewBox.classList.remove('hidden');
            mdWorkspace.classList.add('single');
        } else if (mode === 'edit') {
            editorBox.classList.remove('hidden');
            previewBox.classList.add('hidden');
            mdWorkspace.classList.add('single');
        } else {
            editorBox.classList.remove('hidden');
            previewBox.classList.remove('hidden');
            mdWorkspace.classList.remove('single');
        }
        setLocal(CACHE_VIEW, mode, 365);
    };

    mdInput.addEventListener('input', () => { setLocal(CACHE_MD, mdInput.value); render(); });
    mdViewMode.addEventListener('change', applyView);

    btnSample.addEventListener('click', () => {
        const sample = [
            '# Markdown 示例',
            '',
            '支持 **加粗**、_斜体_、`行内代码`、[链接](https://freecity.club)。',
            '',
            '```javascript',
            'function hello() {',
            '  console.log(\"Hello, Markdown!\");',
            '}',
            '```',
            '',
            '- 列表项 A',
            '- 列表项 B',
            '',
            '> 引用段落',
            '',
            '| 表头A | 表头B |',
            '| ----- | ----- |',
            '| 单元格1 | 单元格2 |'
        ].join('\n');
        mdInput.value = sample;
        setLocal(CACHE_MD, sample);
        render();
    });

    btnPaste.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            mdInput.value = text;
            setLocal(CACHE_MD, text);
            render();
        } catch (e) {}
    });

    btnClear.addEventListener('click', () => {
        mdInput.value = '';
        mdPreview.innerHTML = '';
        localStorage.removeItem(CACHE_MD);
    });

    btnCopyHtml.addEventListener('click', async () => {
        const html = mdPreview.innerHTML;
        if (!html) return;
        try {
            await navigator.clipboard.writeText(html);
        } catch (e) {}
    });

    btnDownloadHtml.addEventListener('click', () => {
        const html = mdPreview.innerHTML;
        const blob = new Blob([html], { type: 'text/html' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'markdown_preview.html';
        document.body.appendChild(a);
        a.click();
        URL.revokeObjectURL(url);
        document.body.removeChild(a);
    });

    render();
    applyView();
});
