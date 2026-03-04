document.addEventListener('DOMContentLoaded', function() {
    const textA = document.getElementById('textA');
    const textB = document.getElementById('textB');
    const pasteA = document.getElementById('pasteA');
    const pasteB = document.getElementById('pasteB');
    const clearA = document.getElementById('clearA');
    const clearB = document.getElementById('clearB');
    const swapBtn = document.getElementById('swapBtn');
    const copyDiffBtn = document.getElementById('copyDiffBtn');
    const stats = document.getElementById('stats');
    const resultContent = document.getElementById('resultContent');
    const resultLeft = document.getElementById('resultLeft');
    const resultRight = document.getElementById('resultRight');
    const ignoreCase = document.getElementById('ignoreCase');
    const ignoreWhitespace = document.getElementById('ignoreWhitespace');
    const viewMode = document.getElementById('viewMode');
    const modeRadios = document.querySelectorAll('input[name="mode"]');

    // 本地缓存
    const CACHE_A = 'tool.diff.textA';
    const CACHE_B = 'tool.diff.textB';
    const getLocal = (k) => (window.btf && window.btf.optLocal) ? window.btf.optLocal.get(k) : localStorage.getItem(k);
    const setLocal = (k, v, ttl = 365) => {
        if (window.btf && window.btf.optLocal) window.btf.optLocal.set(k, v, ttl);
        else localStorage.setItem(k, v);
    };
    const loadCache = () => {
        const a = getLocal(CACHE_A);
        const b = getLocal(CACHE_B);
        if (a) textA.value = typeof a === 'string' ? a : a;
        if (b) textB.value = typeof b === 'string' ? b : b;
    };
    loadCache();

    const saveA = () => setLocal(CACHE_A, textA.value);
    const saveB = () => setLocal(CACHE_B, textB.value);

    function normalize(s) {
        let x = s;
        if (ignoreWhitespace.checked) x = x.replace(/\s+/g, ' ');
        if (ignoreCase.checked) x = x.toLowerCase();
        return x;
    }

    function lcsLines(a, b) {
        const n = a.length, m = b.length;
        const dp = Array(n + 1).fill(0).map(() => Array(m + 1).fill(0));
        for (let i = n - 1; i >= 0; i--) {
            for (let j = m - 1; j >= 0; j--) {
                if (normalize(a[i]) === normalize(b[j])) dp[i][j] = dp[i + 1][j + 1] + 1;
                else dp[i][j] = Math.max(dp[i + 1][j], dp[i][j + 1]);
            }
        }
        const ops = [];
        let i = 0, j = 0;
        while (i < n && j < m) {
            if (normalize(a[i]) === normalize(b[j])) {
                ops.push({ type: 'equal', left: a[i], right: b[j] });
                i++; j++;
            } else if (dp[i + 1][j] >= dp[i][j + 1]) {
                ops.push({ type: 'del', left: a[i], right: '' });
                i++;
            } else {
                ops.push({ type: 'add', left: '', right: b[j] });
                j++;
            }
        }
        while (i < n) { ops.push({ type: 'del', left: a[i], right: '' }); i++; }
        while (j < m) { ops.push({ type: 'add', left: '', right: b[j] }); j++; }
        return ops;
    }

    function diffChars(a, b) {
        const an = normalize(a), bn = normalize(b);
        let p = 0;
        while (p < an.length && p < bn.length && an[p] === bn[p]) p++;
        let s = 0;
        while (s < an.length - p && s < bn.length - p && an[an.length - 1 - s] === bn[bn.length - 1 - s]) s++;
        return {
            prefix: a.slice(0, p),
            aMid: a.slice(p, a.length - s),
            bMid: b.slice(p, b.length - s),
            suffix: a.slice(a.length - s)
        };
    }

    function escapeHtml(x) {
        return x.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function renderSideBySide(ops, charMode) {
        resultLeft.innerHTML = '';
        resultRight.innerHTML = '';
        let eq = 0, add = 0, del = 0, change = 0;
        ops.forEach(op => {
            const l = document.createElement('div');
            const r = document.createElement('div');
            l.className = 'line';
            r.className = 'line';
            if (op.type === 'equal') {
                l.classList.add('equal');
                r.classList.add('equal');
                l.innerHTML = escapeHtml(op.left);
                r.innerHTML = escapeHtml(op.right);
                eq++;
            } else if (op.type === 'add') {
                l.classList.add('change');
                r.classList.add('add');
                if (charMode && op.left !== '' && op.right !== '') {
                    const d = diffChars(op.left, op.right);
                    l.innerHTML = escapeHtml(d.prefix) + '<span class="chunk-del">' + escapeHtml(d.aMid) + '</span>' + escapeHtml(d.suffix);
                    r.innerHTML = escapeHtml(d.prefix) + '<span class="chunk-add">' + escapeHtml(d.bMid) + '</span>' + escapeHtml(d.suffix);
                    change++;
                } else {
                    l.innerHTML = '';
                    r.innerHTML = '<span class="chunk-add">' + escapeHtml(op.right) + '</span>';
                    add++;
                }
            } else if (op.type === 'del') {
                l.classList.add('del');
                r.classList.add('change');
                if (charMode && op.left !== '' && op.right !== '') {
                    const d = diffChars(op.left, op.right);
                    l.innerHTML = escapeHtml(d.prefix) + '<span class="chunk-del">' + escapeHtml(d.aMid) + '</span>' + escapeHtml(d.suffix);
                    r.innerHTML = escapeHtml(d.prefix) + '<span class="chunk-add">' + escapeHtml(d.bMid) + '</span>' + escapeHtml(d.suffix);
                    change++;
                } else {
                    l.innerHTML = '<span class="chunk-del">' + escapeHtml(op.left) + '</span>';
                    r.innerHTML = '';
                    del++;
                }
            }
            resultLeft.appendChild(l);
            resultRight.appendChild(r);
        });
        stats.textContent = eq + ' 行相同 · ' + add + ' 行新增 · ' + del + ' 行删除 · ' + change + ' 行修改';
    }

    function renderInline(ops, charMode) {
        resultContent.style.gridTemplateColumns = '1fr';
        resultRight.innerHTML = '';
        resultLeft.innerHTML = '';
        let eq = 0, add = 0, del = 0, change = 0;
        ops.forEach(op => {
            const l = document.createElement('div');
            l.className = 'line';
            if (op.type === 'equal') {
                l.classList.add('equal');
                l.innerHTML = escapeHtml(op.left);
                eq++;
            } else if (op.type === 'add') {
                l.classList.add('add');
                l.innerHTML = '<span class="chunk-add">' + escapeHtml(op.right) + '</span>';
                add++;
            } else if (op.type === 'del') {
                l.classList.add('del');
                l.innerHTML = '<span class="chunk-del">' + escapeHtml(op.left) + '</span>';
                del++;
            } else {
                l.classList.add('change');
                const d = diffChars(op.left, op.right);
                l.innerHTML = escapeHtml(d.prefix) + '<span class="chunk-del">' + escapeHtml(d.aMid) + '</span>' + '<span class="chunk-add">' + escapeHtml(d.bMid) + '</span>' + escapeHtml(d.suffix);
                change++;
            }
            resultLeft.appendChild(l);
        });
        stats.textContent = eq + ' 行相同 · ' + add + ' 行新增 · ' + del + ' 行删除 · ' + change + ' 行修改';
    }

    function update() {
        const a = textA.value.split('\n');
        const b = textB.value.split('\n');
        const ops = lcsLines(a, b).map(op => {
            if (op.type === 'equal') return op;
            if (op.left !== '' && op.right !== '') return { type: 'change', left: op.left, right: op.right };
            return op;
        });
        const charMode = document.querySelector('input[name="mode"]:checked').value === 'char';
        if (viewMode.value === 'side') {
            resultContent.style.gridTemplateColumns = '1fr 1fr';
            renderSideBySide(ops, charMode);
        } else {
            renderInline(ops, charMode);
        }
    }

    function swapTexts() {
        const a = textA.value;
        textA.value = textB.value;
        textB.value = a;
        saveA(); saveB();
        update();
    }

    async function pasteTo(el) {
        try {
            const t = await navigator.clipboard.readText();
            el.value = t;
            update();
        } catch (e) {}
    }

    function copyDiff() {
        const lines = [];
        const children = resultLeft.children;
        for (let i = 0; i < children.length; i++) {
            const el = children[i];
            const txt = el.textContent;
            lines.push(txt);
        }
        const out = lines.join('\n');
        navigator.clipboard.writeText(out).catch(() => {});
    }

    textA.addEventListener('input', update);
    textB.addEventListener('input', update);
    textA.addEventListener('input', saveA);
    textB.addEventListener('input', saveB);
    ignoreCase.addEventListener('change', update);
    ignoreWhitespace.addEventListener('change', update);
    viewMode.addEventListener('change', update);
    modeRadios.forEach(r => r.addEventListener('change', update));
    swapBtn.addEventListener('click', swapTexts);
    copyDiffBtn.addEventListener('click', copyDiff);
    pasteA.addEventListener('click', () => pasteTo(textA));
    pasteB.addEventListener('click', () => pasteTo(textB));
    clearA.addEventListener('click', () => { textA.value = ''; localStorage.removeItem(CACHE_A); update(); });
    clearB.addEventListener('click', () => { textB.value = ''; localStorage.removeItem(CACHE_B); update(); });
    update();
});
