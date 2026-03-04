document.addEventListener('DOMContentLoaded', function() {
    const fileInput = document.getElementById('fileInput');
    const dropzone = document.getElementById('dropzone');
    const inputList = document.getElementById('inputList');
    const outputList = document.getElementById('outputList');
    const quality = document.getElementById('quality');
    const qualityLabel = document.getElementById('qualityLabel');
    const targetWidthPct = document.getElementById('targetWidthPct');
    const targetFormat = document.getElementById('targetFormat');
    const btnCompressAll = document.getElementById('btnCompressAll');
    const btnDownloadAll = document.getElementById('btnDownloadAll');
    const statInput = document.getElementById('statInput');
    const statOutput = document.getElementById('statOutput');

    const CACHE_OPTS = 'tool.img.compress.options';
    const getLocal = (k) => (window.btf && window.btf.optLocal) ? window.btf.optLocal.get(k) : localStorage.getItem(k);
    const setLocal = (k, v, ttl = 365) => {
        if (window.btf && window.btf.optLocal) window.btf.optLocal.set(k, v, ttl);
        else localStorage.setItem(k, v);
    };

    const saved = getLocal(CACHE_OPTS);
    if (saved) {
        try {
            const opts = typeof saved === 'string' ? JSON.parse(saved) : saved;
            if (quality && opts.quality) quality.value = opts.quality;
            if (targetWidthPct && typeof opts.widthPct !== 'undefined' && opts.widthPct !== null) targetWidthPct.value = opts.widthPct;
            if (targetFormat && opts.format) targetFormat.value = opts.format;
        } catch (e) {}
    }
    if (qualityLabel && quality) qualityLabel.textContent = quality.value + '%';

    function saveOpts() {
        const qVal = quality ? Number(quality.value) : 80;
        const wVal = targetWidthPct ? (targetWidthPct.value ? Math.max(0, Math.min(100, Number(targetWidthPct.value))) : 100) : 100;
        const fVal = targetFormat ? targetFormat.value : 'image/jpeg';
        const opts = { quality: qVal, widthPct: wVal, format: fVal };
        setLocal(CACHE_OPTS, JSON.stringify(opts), 365);
    }

    const files = [];
    const results = [];

    if (dropzone) dropzone.addEventListener('click', () => fileInput && fileInput.click());
    if (fileInput) fileInput.addEventListener('change', (e) => handleFiles(e.target.files));

    if (dropzone) dropzone.addEventListener('dragover', (e) => {
        e.preventDefault();
        dropzone.style.background = '#f0f7ff';
    });
    if (dropzone) dropzone.addEventListener('dragleave', () => {
        dropzone.style.background = '#fbfbfb';
    });
    if (dropzone) dropzone.addEventListener('drop', (e) => {
        e.preventDefault();
        dropzone.style.background = '#fbfbfb';
        const dtFiles = e.dataTransfer.files;
        handleFiles(dtFiles);
    });

    if (quality) quality.addEventListener('input', () => {
        const val = Math.max(0, Math.min(100, Number(quality.value)));
        quality.value = val;
        if (qualityLabel) qualityLabel.textContent = val + '%';
        saveOpts();
    });
    if (targetWidthPct) targetWidthPct.addEventListener('input', () => {
        // Clamp and save
        const v = Math.max(0, Math.min(100, Number(targetWidthPct.value || 100)));
        targetWidthPct.value = v;
        saveOpts();
    });
    if (targetFormat) targetFormat.addEventListener('change', saveOpts);

    function bytes(n) {
        if (n < 1024) return n + ' B';
        if (n < 1024 * 1024) return (n / 1024).toFixed(1) + ' KB';
        return (n / (1024 * 1024)).toFixed(2) + ' MB';
    }

    function handleFiles(list) {
        for (let f of list) {
            if (!f.type.startsWith('image/')) continue;
            files.push(f);
        }
        renderInputList();
    }

    function renderInputList() {
        statInput.textContent = files.length + '张';
        inputList.innerHTML = '';
        const frag = document.createDocumentFragment();
        files.forEach((f, idx) => {
            const card = document.createElement('div');
            card.className = 'file-card';
            card.innerHTML = `
                <div class="thumb"><img alt=""></div>
                <div class="meta"><span class="name" title="${f.name}">${f.name}</span><span class="size">${bytes(f.size)}</span></div>
                <div class="actions">
                  <button class="btn-ghost" data-idx="${idx}">压缩</button>
                </div>
            `;
            const imgEl = card.querySelector('img');
            const reader = new FileReader();
            reader.onload = (e) => { imgEl.src = e.target.result; };
            reader.readAsDataURL(f);
            const btn = card.querySelector('button');
            btn.addEventListener('click', () => compressOne(idx));
            imgEl.addEventListener('click', () => openModal(imgEl.src, f.name));
            frag.appendChild(card);
        });
        inputList.appendChild(frag);
    }

    async function loadImage(file) {
        const blobURL = URL.createObjectURL(file);
        let img;
        try {
            img = await createImageBitmap(file);
        } catch (e) {
            img = await new Promise((resolve, reject) => {
                const image = new Image();
                image.onload = () => resolve(image);
                image.onerror = reject;
                image.src = blobURL;
            });
        } finally {
            URL.revokeObjectURL(blobURL);
        }
        return img;
    }

    async function canvasToBlob(canvas, format, q) {
        return await new Promise(resolve => {
            canvas.toBlob(b => resolve(b || null), format, q);
        });
    }
    async function blobToDataURL(blob) {
        return await new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = () => resolve(reader.result);
            reader.onerror = reject;
            reader.readAsDataURL(blob);
        });
    }

    async function compress(file) {
        const q = quality ? Number(quality.value) / 100 : 0.8;
        const format = targetFormat ? targetFormat.value : 'image/jpeg';
        const pct = targetWidthPct ? (targetWidthPct.value ? Math.max(0, Math.min(100, Number(targetWidthPct.value))) : 100) : 100;

        const source = await loadImage(file);
        const srcW = source.width, srcH = source.height;
        let dstW = Math.round(srcW * (pct / 100));
        if (dstW < 1) dstW = 1;
        const dstH = Math.round(srcH * (dstW / srcW));

        const canvas = document.createElement('canvas');
        canvas.width = dstW;
        canvas.height = dstH;
        const ctx = canvas.getContext('2d');
        ctx.drawImage(source, 0, 0, dstW, dstH);
        let blob = await canvasToBlob(canvas, format, q);
        if (!blob) {
            // Fallback: 尝试 JPEG，其次 PNG
            blob = await canvasToBlob(canvas, 'image/jpeg', q);
            if (!blob) blob = await canvasToBlob(canvas, 'image/png', 1);
        }
        const nameNoExt = file.name.replace(/\.[^.]+$/, '');
        const outFmt = blob && blob.type ? blob.type : format;
        const ext = outFmt === 'image/png' ? 'png' : (outFmt === 'image/webp' ? 'webp' : 'jpg');
        const outName = `${nameNoExt}_${dstW}w_q${Math.round(q*100)}.${ext}`;
        const dataUrl = await blobToDataURL(blob);
        return { blob, dataUrl, name: outName, originalSize: file.size, originalFile: file };
    }

    async function compressOne(idx) {
        const f = files[idx];
        const result = await compress(f);
        results.push(result);
        renderOutputList();
    }

    btnCompressAll.addEventListener('click', async () => {
        results.length = 0;
        for (let i = 0; i < files.length; i++) {
            const r = await compress(files[i]);
            results.push(r);
        }
        renderOutputList();
    });

    function renderOutputList() {
        statOutput.textContent = results.length + '张';
        outputList.innerHTML = '';
        const frag = document.createDocumentFragment();
        results.forEach((r, idx) => {
            const original = r.originalSize;
            const compressed = r.blob.size;
            const pctChange = original > 0 ? Math.round(Math.abs(compressed - original) / original * 100) : 0;
            const trend = compressed > original ? '↑' : '↓';
            const card = document.createElement('div');
            card.className = 'file-card';
            card.innerHTML = `
                <div class="thumb"><img src="${r.dataUrl}" alt=""></div>
                <div class="meta"><span class="name" title="${r.name}">${r.name}</span><span class="size">${bytes(original)} → ${bytes(compressed)} (${trend}${pctChange}%)</span></div>
                <div class="actions">
                  <a class="btn-ghost" href="${r.dataUrl}" download="${r.name}">下载</a>
                </div>
            `;
            const thumbImg = card.querySelector('.thumb img');
            thumbImg.addEventListener('click', () => openModal(r.dataUrl, r.name));
            frag.appendChild(card);
        });
        outputList.appendChild(frag);
    }

    // Modal preview
    const modal = document.getElementById('imgModal');
    const modalImg = document.getElementById('imgModalImg');
    const modalCaption = document.getElementById('imgModalCaption');
    const modalClose = document.getElementById('imgModalClose');
    function openModal(src, caption) {
        modalImg.src = src;
        modalCaption.textContent = caption || '';
        modal.style.display = 'flex';
        document.addEventListener('keydown', escHandler);
    }
    function closeModal() {
        modal.style.display = 'none';
        modalImg.src = '';
        modalCaption.textContent = '';
        document.removeEventListener('keydown', escHandler);
    }
    function escHandler(e) {
        if (e.key === 'Escape') closeModal();
    }
    modalClose.addEventListener('click', closeModal);
    modal.addEventListener('click', (e) => {
        if (e.target === modal) closeModal();
    });

    btnDownloadAll.addEventListener('click', () => {
        results.forEach(r => {
            const url = URL.createObjectURL(r.blob);
            const a = document.createElement('a');
            a.href = url;
            a.download = r.name;
            document.body.appendChild(a);
            a.click();
            URL.revokeObjectURL(url);
            document.body.removeChild(a);
        });
    });
});
