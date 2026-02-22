/**
 * JSON 格式化工具核心逻辑
 */

document.addEventListener('DOMContentLoaded', function() {
    const jsonInput = document.getElementById('jsonInput');
    const jsonOutput = document.getElementById('jsonOutput');
    
    // 按钮元素
    const btnFormat = document.getElementById('btnFormat');
    const btnCompress = document.getElementById('btnCompress');
    const btnEscape = document.getElementById('btnEscape');
    const btnUnescape = document.getElementById('btnUnescape');
    const btnToXml = document.getElementById('btnToXml');
    const btnXmlToJson = document.getElementById('btnXmlToJson');
    const btnToYaml = document.getElementById('btnToYaml');
    const btnYamlToJson = document.getElementById('btnYamlToJson');
    const btnClear = document.getElementById('btnClear');
    const btnPaste = document.getElementById('btnPaste');
    const btnCopy = document.getElementById('btnCopy');
    const btnSample = document.getElementById('btnSample');
    const btnDownload = document.getElementById('btnDownload');

    // 缓存 Key
    const CACHE_KEY = 'json_format_input_cache';

    // 按钮样式切换逻辑
    const toolButtons = document.querySelectorAll('.tool-controls .btn-tool');
    function setActiveButton(btn) {
        toolButtons.forEach(b => b.classList.remove('primary'));
        btn.classList.add('primary');
    }

    // 保存到本地缓存
    function saveToCache() {
        const input = jsonInput.value;
        // 如果内容过大（超过 2MB），不建议存入 localStorage
        if (input.length > 2 * 1024 * 1024) {
            console.warn('Content too large, not saving to cache');
            return;
        }
        localStorage.setItem(CACHE_KEY, input);
    }

    // 从本地缓存加载
    function loadFromCache() {
        const cachedValue = localStorage.getItem(CACHE_KEY);
        if (cachedValue) {
            jsonInput.value = cachedValue;
            updateButtonStates();
        }
    }

    // 按钮状态更新逻辑
    function updateButtonStates() {
        const input = jsonInput.value.trim();
        
        // 初始状态：全部禁用（除了示例和粘贴）
        const allActionButtons = [
            btnFormat, btnCompress, btnEscape, btnUnescape, 
            btnToXml, btnXmlToJson, btnToYaml, btnYamlToJson
        ];
        
        if (!input) {
            allActionButtons.forEach(btn => {
                btn.disabled = true;
                btn.classList.add('disabled');
                btn.classList.remove('primary'); // 禁用时移除高亮
            });
            return;
        }

        // 识别格式
        const isJson = (input.startsWith('{') && input.endsWith('}')) || (input.startsWith('[') && input.endsWith(']'));
        const isXml = input.startsWith('<') && input.endsWith('>');
        const isYaml = !isJson && !isXml && (input.includes(': ') || input.startsWith('- '));

        // 默认先禁用所有功能按钮（除了示例数据）
        allActionButtons.forEach(btn => {
            if (btn !== btnSample) {
                btn.disabled = true;
                btn.classList.add('disabled');
            }
        });

        // 通用按钮：只要有输入，转义/去转义始终可用
        [btnEscape, btnUnescape].forEach(btn => {
            btn.disabled = false;
            btn.classList.remove('disabled');
        });

        if (isJson) {
            // JSON: 格式化、压缩、转 XML、转 YAML
            [btnFormat, btnCompress, btnToXml, btnToYaml].forEach(btn => {
                btn.disabled = false;
                btn.classList.remove('disabled');
            });
        } else if (isXml) {
            // XML: 格式化、压缩、转 JSON
            [btnFormat, btnCompress, btnXmlToJson].forEach(btn => {
                btn.disabled = false;
                btn.classList.remove('disabled');
            });
        } else if (isYaml) {
            // YAML: 格式化、转 JSON
            [btnFormat, btnYamlToJson].forEach(btn => {
                btn.disabled = false;
                btn.classList.remove('disabled');
            });
        }

        // 最后统一检查：如果按钮被禁用了，必须移除 primary 样式
        allActionButtons.forEach(btn => {
            if (btn.disabled) {
                btn.classList.remove('primary');
            }
        });
    }

    // 监听输入变化
    jsonInput.addEventListener('input', function() {
        updateButtonStates();
        saveToCache();
    });
    
    // 初始化时从缓存加载
    loadFromCache();
    
    // 初始化按钮状态
    updateButtonStates();

    // 1. 格式化 JSON/XML/YAML
    btnFormat.addEventListener('click', function(e) {
        if (e && e.isTrusted) setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        // 重新识别当前输入的格式
        const isJson = (input.startsWith('{') && input.endsWith('}')) || (input.startsWith('[') && input.endsWith(']'));
        const isXml = input.startsWith('<'); // XML 通常以 < 开头
        
        try {
            if (isJson) {
                const obj = JSON.parse(input);
                jsonOutput.value = JSON.stringify(obj, null, 4);
                showToast('JSON 格式化成功', 'success');
            } else if (isXml) {
                jsonOutput.value = formatXml(input);
                showToast('XML 格式化成功', 'success');
            } else {
                // 尝试作为 YAML 处理
                if (window.jsyaml) {
                    const obj = window.jsyaml.load(input);
                    jsonOutput.value = window.jsyaml.dump(obj, {
                        indent: 2,
                        lineWidth: -1, // 不自动换行
                        noRefs: true   // 不使用引用
                    });
                    showToast('YAML 格式化成功', 'success');
                } else {
                    throw new Error('YAML 库未加载');
                }
            }
        } catch (err) {
            console.error('Format error:', err);
            jsonOutput.value = '错误: ' + err.message;
            showToast('格式化失败', 'error');
        }
    });

    // 2. 压缩 JSON/XML
    btnCompress.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        if (input.startsWith('<')) {
            // XML 压缩
            try {
                jsonOutput.value = input.replace(/>\s+</g, '><').trim();
                showToast('XML 压缩成功', 'success');
            } catch (e) {
                showToast('XML 压缩失败', 'error');
            }
        } else {
            // JSON 压缩
            try {
                const obj = JSON.parse(input);
                jsonOutput.value = JSON.stringify(obj);
                showToast('压缩成功', 'success');
            } catch (e) {
                jsonOutput.value = '错误: ' + e.message;
                showToast('JSON 格式无效', 'error');
            }
        }
    });

    // 3. 转义 JSON
    btnEscape.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        jsonOutput.value = JSON.stringify(input).slice(1, -1);
        showToast('转义成功', 'success');
    });

    // 4. 去转义 JSON
    btnUnescape.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        try {
            // 简单的去转义逻辑
            jsonOutput.value = input.replace(/\\"/g, '"').replace(/\\\\/g, '\\');
            showToast('去转义成功', 'success');
        } catch (e) {
            showToast('去转义失败', 'error');
        }
    });

    // 5. 转 XML
    btnToXml.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        try {
            const obj = JSON.parse(input);
            const xmlContent = jsonToXml(obj, 'root');
            jsonOutput.value = '<?xml version="1.0" encoding="UTF-8"?>\n' + formatXml(xmlContent);
            showToast('已转换为 XML', 'success');
        } catch (e) {
            jsonOutput.value = '错误: ' + e.message;
            showToast('JSON 格式无效', 'error');
        }
    });

    // 6. XML 转 JSON
    btnXmlToJson.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        try {
            // 根据官方文档，V4 版本的浏览器 bundle (fxp.min.js) 会在 window.fxparser 下挂载
            // 我们尝试所有可能的挂载点以确保兼容性
            const XMLParserClass = (window.fxparser && window.fxparser.XMLParser) || 
                                 (window.fxp && window.fxp.XMLParser) ||
                                 (window.fastXmlParser && window.fastXmlParser.XMLParser) ||
                                 window.XMLParser;
            
            if (XMLParserClass) {
                const parser = new XMLParserClass({
                    ignoreAttributes: false,
                    attributeNamePrefix: "@_",
                    ignoreDeclaration: true,
                    ignorePIs: true
                });
                let jsonObj = parser.parse(input);
                
                // 移除 root 顶层容器：如果结果只有一个键（即 root 元素），则提取其内容
                const keys = Object.keys(jsonObj);
                if (keys.length === 1) {
                    jsonObj = jsonObj[keys[0]];
                }

                jsonOutput.value = JSON.stringify(jsonObj, null, 4);
                showToast('已转换为 JSON', 'success');
            } else {
                showToast('XML 解析库加载失败，请尝试刷新页面', 'error');
            }
        } catch (e) {
            console.error('XML parse error:', e);
            jsonOutput.value = '错误: ' + e.message;
            showToast('XML 格式无效', 'error');
        }
    });

    // 7. 转 YAML
    btnToYaml.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        try {
            const obj = JSON.parse(input);
            if (window.jsyaml) {
                jsonOutput.value = window.jsyaml.dump(obj);
                showToast('已转换为 YAML', 'success');
            } else {
                showToast('YAML 库加载失败', 'error');
            }
        } catch (e) {
            jsonOutput.value = '错误: ' + e.message;
            showToast('JSON 格式无效', 'error');
        }
    });

    // 9. YAML 转 JSON
    btnYamlToJson.addEventListener('click', function() {
        setActiveButton(this);
        const input = jsonInput.value.trim();
        if (!input) return;
        
        try {
            if (window.jsyaml) {
                const jsonObj = window.jsyaml.load(input);
                jsonOutput.value = JSON.stringify(jsonObj, null, 4);
                showToast('已转换为 JSON', 'success');
            } else {
                showToast('YAML 库加载失败', 'error');
            }
        } catch (e) {
            jsonOutput.value = '错误: ' + e.message;
            showToast('YAML 格式无效', 'error');
        }
    });

    // 10. 清空
    btnClear.addEventListener('click', function() {
        jsonInput.value = '';
        jsonOutput.value = '';
        localStorage.removeItem(CACHE_KEY); // 清除缓存
        updateButtonStates();
        showToast('内容已清空', 'info');
    });

    // 8. 粘贴
    btnPaste.addEventListener('click', async function() {
        try {
            const text = await navigator.clipboard.readText();
            jsonInput.value = text;
            updateButtonStates();
            saveToCache(); // 粘贴后存入缓存
            showToast('已从剪贴板粘贴', 'info');
        } catch (err) {
            showToast('粘贴失败，请手动输入', 'error');
        }
    });

    // 9. 一键复制
    btnCopy.addEventListener('click', function() {
        const text = jsonOutput.value;
        if (!text) return;
        
        navigator.clipboard.writeText(text).then(() => {
            showToast('复制成功', 'success');
        }).catch(err => {
            showToast('复制失败', 'error');
        });
    });

    // 10. 示例数据
    btnSample.addEventListener('click', function() {
        setActiveButton(this);
        const sample = {
            "project": "FreeCity Online Tools",
            "version": "1.0.0",
            "author": "wyc",
            "features": [
                "JSON Format",
                "Base64",
                "Timestamp",
                "Image Compress"
            ],
            "active": true,
            "stats": {
                "users": 1000,
                "uptime": "99.9%"
            }
        };
        jsonInput.value = JSON.stringify(sample, null, 4);
        updateButtonStates();
        // saveToCache(); // 加载示例后存入缓存
        btnFormat.click();
        showToast('示例数据已加载', 'info');
    });

    // 11. 下载
    btnDownload.addEventListener('click', function() {
        const text = jsonOutput.value;
        if (!text) return;
        
        const blob = new Blob([text], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'data.json';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        URL.revokeObjectURL(url);
    });

    // 辅助：Toast 提示 (简单实现，如果项目有自己的 UI 库可以替换)
    function showToast(message, type = 'info') {
        // 如果项目中已引入 snackbar，则优先使用
        if (window.Snackbar) {
            window.Snackbar.show({
                text: message,
                pos: 'top-center',
                duration: 2500,
                showAction: false
            });
        } else {
            console.log(`[${type}] ${message}`);
            // 这里可以添加一个简单的 DOM Toast 实现
            const toast = document.createElement('div');
            toast.style.cssText = `
                position: fixed;
                top: 20px;
                left: 50%;
                transform: translateX(-50%);
                padding: 10px 20px;
                background: var(--text-highlight-color);
                color: #fff;
                border-radius: 8px;
                z-index: 9999;
                box-shadow: 0 4px 12px rgba(0,0,0,0.15);
                font-size: 0.9rem;
                animation: fadeInDown 0.3s;
            `;
            toast.textContent = message;
            document.body.appendChild(toast);
            setTimeout(() => {
                toast.style.animation = 'fadeOutUp 0.3s';
                setTimeout(() => document.body.removeChild(toast), 300);
            }, 2500);
        }
    }
});

// 动画样式补丁
const style = document.createElement('style');
style.textContent = `
    @keyframes fadeInDown {
        from { opacity: 0; transform: translate(-50%, -20px); }
        to { opacity: 1; transform: translate(-50%, 0); }
    }
    @keyframes fadeOutUp {
        from { opacity: 1; transform: translate(-50%, 0); }
        to { opacity: 0; transform: translate(-50%, -20px); }
    }
    .btn-tool.disabled {
        opacity: 0.5;
        cursor: not-allowed;
        pointer-events: none;
        filter: grayscale(1);
    }
`;
document.head.appendChild(style);

/**
 * 将 JSON 对象转换为 XML 字符串
 */
function jsonToXml(obj, rootName) {
    let xml = '';
    if (obj instanceof Array) {
        for (let i = 0; i < obj.length; i++) {
            xml += jsonToXml(obj[i], 'item');
        }
    } else if (typeof obj === 'object' && obj !== null) {
        for (let prop in obj) {
            if (obj.hasOwnProperty(prop)) {
                xml += jsonToXml(obj[prop], prop);
            }
        }
    } else {
        xml += obj;
    }
    return rootName ? `<${rootName}>${xml}</${rootName}>` : xml;
}

/**
 * 格式化 XML 字符串（添加缩进）
 */
/**
 * 格式化 XML 字符串
 * 使用更严谨的正则表达式拆分标签，并逐级计算缩进
 * @param {string} xml 原始 XML 字符串
 * @returns {string} 格式化后的 XML 字符串
 */
function formatXml(xml) {
    const PADDING = '    '; // 4个空格缩进
    let formatted = '';
    let pad = 0;

    // 1. 清理：去掉标签间的换行和多余空格，统一为单行流
    xml = xml.replace(/>\s{0,}</g, '><').trim();

    // 2. 拆分：利用正则将 XML 拆分为标签块和文本块
    // ([^<]+) 匹配标签间的文本
    // (<[^>]+>) 匹配完整的标签（包括声明、注释、自闭合、开关标签）
    const nodes = xml.split(/(<[^>]+>)/g).filter(n => n.trim() !== '');

    nodes.forEach(node => {
        // 处理结束标签 </tag>
        if (node.match(/^<\/\w/)) {
            if (pad !== 0) pad--;
            formatted += PADDING.repeat(pad) + node + '\n';
        }
        // 处理开始标签 <tag> 或 <tag />
        else if (node.match(/^<\w[^>]*>/)) {
            formatted += PADDING.repeat(pad) + node + '\n';
            
            // 如果不是自闭合标签且不是声明/注释，则增加缩进
            // 排除 <?...?> (声明), <!--...--> (注释), <tag /> (自闭合)
            const isSelfClosing = node.match(/\/>$/);
            const isDeclaration = node.match(/^<\?/);
            const isComment = node.match(/^<!--/);
            
            if (!isSelfClosing && !isDeclaration && !isComment) {
                pad++;
            }
        }
        // 处理文本内容
        else {
            formatted += PADDING.repeat(pad) + node.trim() + '\n';
        }
    });

    // 3. 优化：合并被拆分的“开始标签+文本+结束标签”到同一行
    // 例如将：
    // <name>
    //     John
    // </name>
    // 优化为：
    // <name>John</name>
    const lines = formatted.trim().split('\n');
    const optimized = [];
    for (let i = 0; i < lines.length; i++) {
        const current = lines[i];
        const next = lines[i + 1];
        const third = lines[i + 2];

        if (next && third && 
            current.match(/^(\s*)<\w[^>]*[^/]>$/) && // 是开始标签
            !next.match(/<[^>]+>/) &&               // 下一行是纯文本
            third.match(/^(\s*)<\/\w+>$/) &&        // 再下一行是对应的结束标签
            current.trim().split(' ')[0].replace(/[<>]/g, '') === third.trim().replace(/[<>/]/g, '') // 标签名匹配
        ) {
            optimized.push(current.trimEnd() + next.trim() + third.trim());
            i += 2; // 跳过后续两行
        } else {
            optimized.push(current);
        }
    }

    return optimized.join('\n');
}
