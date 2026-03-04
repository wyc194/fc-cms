/**
 * 二维码生成器工具核心逻辑
 */

document.addEventListener('DOMContentLoaded', function() {
    const qrInput = document.getElementById('qrInput');
    const qrCanvas = document.getElementById('qrCanvas');
    const qrSize = document.getElementById('qrSize');
    const qrErrorLevel = document.getElementById('qrErrorLevel');
    const qrColorDark = document.getElementById('qrColorDark');
    const qrColorDarkHex = document.getElementById('qrColorDarkHex');
    const qrColorLight = document.getElementById('qrColorLight');
    const qrColorLightHex = document.getElementById('qrColorLightHex');
    const qrMargin = document.getElementById('qrMargin');
    
    // 按钮元素
    const btnGenerate = document.getElementById('btnGenerate');
    const btnClear = document.getElementById('btnClear');
    const btnPaste = document.getElementById('btnPaste');
    const btnDownload = document.getElementById('btnDownload');
    const btnSample = document.getElementById('btnSample');

    // 缓存 Key
    const CACHE_KEY = 'qr_code_input_cache';

    // 颜色同步逻辑
    function syncColor(source, target) {
        target.value = source.value;
        generateQRCode();
    }

    qrColorDark.addEventListener('input', () => syncColor(qrColorDark, qrColorDarkHex));
    qrColorDarkHex.addEventListener('input', () => syncColor(qrColorDarkHex, qrColorDark));
    qrColorLight.addEventListener('input', () => syncColor(qrColorLight, qrColorLightHex));
    qrColorLightHex.addEventListener('input', () => syncColor(qrColorLightHex, qrColorLight));

    // 生成二维码
    function generateQRCode() {
        const text = qrInput.value.trim();
        if (!text) {
            // 如果没有内容，清空画布
            const ctx = qrCanvas.getContext('2d');
            ctx.clearRect(0, 0, qrCanvas.width, qrCanvas.height);
            return;
        }

        const options = {
            width: parseInt(qrSize.value) || 300,
            margin: parseInt(qrMargin.value) || 2,
            errorCorrectionLevel: qrErrorLevel.value || 'M',
            color: {
                dark: qrColorDarkHex.value || '#000000',
                light: qrColorLightHex.value || '#ffffff'
            }
        };

        const qrLib = window.QRCode || (typeof QRCode !== 'undefined' ? QRCode : null);
        if (qrLib) {
            qrLib.toCanvas(qrCanvas, text, options, function(error) {
                if (error) {
                    console.error('QR Code generation error:', error);
                    if (typeof showToast === 'function') showToast('二维码生成失败: ' + error.message, 'error');
                } else {
                    saveToCache();
                }
            });
        } else {
            console.error('QRCode library not loaded');
            if (typeof showToast === 'function') showToast('库加载失败，请刷新重试', 'error');
        }
    }

    // 保存到本地缓存
    function saveToCache() {
        const input = qrInput.value;
        if (input.length > 100 * 1024) return;
        localStorage.setItem(CACHE_KEY, input);
    }

    // 从本地缓存加载
    function loadFromCache() {
        const cachedValue = localStorage.getItem(CACHE_KEY);
        if (cachedValue) {
            qrInput.value = cachedValue;
            generateQRCode();
        }
    }

    // 监听输入变化
    let debounceTimer;
    qrInput.addEventListener('input', function() {
        clearTimeout(debounceTimer);
        debounceTimer = setTimeout(generateQRCode, 300);
    });

    [qrSize, qrErrorLevel, qrMargin].forEach(el => {
        el.addEventListener('change', generateQRCode);
    });

    // 1. 重新生成
    btnGenerate.addEventListener('click', generateQRCode);

    // 2. 清空
    btnClear.addEventListener('click', function() {
        qrInput.value = '';
        generateQRCode();
        localStorage.removeItem(CACHE_KEY);
        if (typeof showToast === 'function') showToast('内容已清空', 'success');
    });

    // 3. 粘贴
    btnPaste.addEventListener('click', async function() {
        try {
            const text = await navigator.clipboard.readText();
            qrInput.value = text;
            generateQRCode();
            if (typeof showToast === 'function') showToast('粘贴成功', 'success');
        } catch (err) {
            if (typeof showToast === 'function') showToast('无法访问剪贴板', 'error');
        }
    });

    // 4. 下载
    btnDownload.addEventListener('click', function() {
        const text = qrInput.value.trim();
        if (!text) {
            if (typeof showToast === 'function') showToast('请先输入内容', 'warning');
            return;
        }
        
        try {
            const link = document.createElement('a');
            link.download = `qrcode_${Date.now()}.png`;
            link.href = qrCanvas.toDataURL('image/png');
            link.click();
            if (typeof showToast === 'function') showToast('下载成功', 'success');
        } catch (e) {
            if (typeof showToast === 'function') showToast('下载失败', 'error');
        }
    });

    // 5. 示例数据
    btnSample.addEventListener('click', function() {
        qrInput.value = 'https://freecity.club';
        generateQRCode();
    });

    // 辅助：Toast 提示
    function showToast(message, type = 'info') {
        if (window.Snackbar) {
            window.Snackbar.show({
                text: message,
                pos: 'bottom-center',
                showAction: false,
                duration: 2000,
                backgroundColor: type === 'success' ? '#52c41a' : (type === 'error' ? '#ff4d4f' : '#333')
            });
        } else if (window.btf && window.btf.snackbar) {
            window.btf.snackbar.show(message, type);
        } else {
            alert(message);
        }
    }

    // 初始化
    loadFromCache();
    if (!qrInput.value) {
        qrInput.value = 'https://freecity.club';
        generateQRCode();
    }
});
