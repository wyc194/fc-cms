document.addEventListener('DOMContentLoaded', function() {
    // 元素获取
    const inputArea = document.getElementById('inputArea');
    const outputArea = document.getElementById('outputArea');
    const btnEncode = document.getElementById('btnEncode');
    const btnDecode = document.getElementById('btnDecode');
    const btnUrlEncode = document.getElementById('btnUrlEncode');
    const btnUrlDecode = document.getElementById('btnUrlDecode');
    const btnUpload = document.getElementById('btnUpload');
    const fileInput = document.getElementById('fileInput');
    const btnPaste = document.getElementById('btnPaste');
    const btnClear = document.getElementById('btnClear');
    const btnCopy = document.getElementById('btnCopy');
    const btnDownload = document.getElementById('btnDownload');
    const chkSplit = document.getElementById('chkSplit');

    // 工具函数：UTF-8 转 Base64
    function utf8_to_b64(str) {
        return window.btoa(unescape(encodeURIComponent(str)));
    }

    // 工具函数：Base64 转 UTF-8
    function b64_to_utf8(str) {
        return decodeURIComponent(escape(window.atob(str)));
    }

    // 格式化输出 (76字符换行)
    function formatBase64(str) {
        if (!chkSplit.checked) return str;
        return str.replace(/(.{76})/g, "$1\n");
    }

    // 清理非 Base64 字符
    function cleanBase64(str) {
        return str.replace(/[^A-Za-z0-9+/=]/g, "");
    }

    // 绑定事件
    
    // 编码
    btnEncode.addEventListener('click', () => {
        const input = inputArea.value;
        if (!input) return;
        try {
            const encoded = utf8_to_b64(input);
            outputArea.value = formatBase64(encoded);
        } catch (e) {
            outputArea.value = "编码失败：" + e.message;
        }
    });

    // 解码
    btnDecode.addEventListener('click', () => {
        let input = inputArea.value.trim();
        if (!input) return;
        try {
            // 如果包含换行符，先清理
            input = cleanBase64(input);
            const decoded = b64_to_utf8(input);
            outputArea.value = decoded;
        } catch (e) {
            outputArea.value = "解码失败：输入的不是有效的 Base64 字符串";
        }
    });

    // URL 安全编码
    btnUrlEncode.addEventListener('click', () => {
        const input = inputArea.value;
        if (!input) return;
        try {
            let encoded = utf8_to_b64(input);
            encoded = encoded.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
            outputArea.value = encoded;
        } catch (e) {
            outputArea.value = "编码失败：" + e.message;
        }
    });

    // URL 安全解码
    btnUrlDecode.addEventListener('click', () => {
        let input = inputArea.value.trim();
        if (!input) return;
        try {
            // 还原 URL 安全字符
            input = input.replace(/-/g, '+').replace(/_/g, '/');
            // 补全 padding
            while (input.length % 4) {
                input += '=';
            }
            const decoded = b64_to_utf8(input);
            outputArea.value = decoded;
        } catch (e) {
            outputArea.value = "解码失败：输入的不是有效的 Base64 字符串";
        }
    });

    // 文件上传转 Base64
    btnUpload.addEventListener('click', () => fileInput.click());
    
    fileInput.addEventListener('change', (e) => {
        const file = e.target.files[0];
        if (!file) return;

        const reader = new FileReader();
        reader.onload = function(e) {
            const result = e.target.result;
            // result 格式如: data:image/png;base64,iVBORw0KGgo...
            // 提取纯 Base64 部分用于显示，但也保留完整 Data URI 供用户选择
            const base64Part = result.split(',')[1];
            
            inputArea.value = `[文件: ${file.name}, 大小: ${(file.size/1024).toFixed(2)}KB]`;
            outputArea.value = formatBase64(base64Part);
            
            // 提示用户
            // alert("文件转换成功！已显示 Base64 编码内容。");
        };
        reader.onerror = function() {
            outputArea.value = "文件读取失败";
        };
        reader.readAsDataURL(file);
        
        // 清空 input 允许再次选择同名文件
        fileInput.value = '';
    });

    // 粘贴
    btnPaste.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            inputArea.value = text;
        } catch (err) {
            console.error('无法读取剪贴板内容', err);
            inputArea.focus();
            document.execCommand('paste');
        }
    });

    // 清空
    btnClear.addEventListener('click', () => {
        inputArea.value = '';
        outputArea.value = '';
        inputArea.focus();
    });

    // 复制结果
    btnCopy.addEventListener('click', () => {
        if (!outputArea.value) return;
        outputArea.select();
        document.execCommand('copy');
        // 简单的反馈
        const originalText = btnCopy.innerHTML;
        btnCopy.innerHTML = '<i class="fas fa-check"></i>';
        setTimeout(() => btnCopy.innerHTML = originalText, 1500);
    });

    // 下载结果
    btnDownload.addEventListener('click', () => {
        const content = outputArea.value;
        if (!content) return;
        
        const blob = new Blob([content], { type: 'text/plain' });
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'base64_result.txt';
        document.body.appendChild(a);
        a.click();
        window.URL.revokeObjectURL(url);
        document.body.removeChild(a);
    });
});
