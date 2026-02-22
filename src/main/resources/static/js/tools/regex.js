document.addEventListener('DOMContentLoaded', function() {
    // 元素获取
    const regexPattern = document.getElementById('regexPattern');
    const regexError = document.getElementById('regexError');
    const testString = document.getElementById('testString');
    const matchHighlights = document.getElementById('matchHighlights');
    const matchInfo = document.getElementById('matchInfo');
    
    // 标志位复选框
    const flags = {
        global: document.getElementById('flagGlobal'),
        insensitive: document.getElementById('flagInsensitive'),
        multiline: document.getElementById('flagMultiline'),
        dotAll: document.getElementById('flagDotAll'),
        unicode: document.getElementById('flagUnicode'),
        sticky: document.getElementById('flagSticky')
    };

    // 按钮
    const btnPaste = document.getElementById('btnPaste');
    const btnClear = document.getElementById('btnClear');
    const btnCopyRegex = document.getElementById('btnCopyRegex');

    // Event Listeners
    regexPattern.addEventListener('input', updateResult);
    testString.addEventListener('input', updateResult);
    testString.addEventListener('scroll', syncScroll);
    
    // Resize observer to sync height when textarea is resized
    const resizeObserver = new ResizeObserver(() => {
        matchHighlights.style.height = testString.style.height;
    });
    resizeObserver.observe(testString);

    Object.values(flags).forEach(checkbox => {
        checkbox.addEventListener('change', updateResult);
    });

    btnClear.addEventListener('click', () => {
        testString.value = '';
        regexPattern.value = '';
        updateResult();
        testString.focus();
    });

    btnPaste.addEventListener('click', async () => {
        try {
            const text = await navigator.clipboard.readText();
            testString.value = text;
            updateResult();
        } catch (err) {
            console.error('Failed to read clipboard:', err);
            testString.focus();
            document.execCommand('paste');
        }
    });

    // Handle preset buttons
    document.querySelectorAll('.btn-regex-preset').forEach(btn => {
        btn.addEventListener('click', () => {
            regexPattern.value = btn.dataset.regex;
            const flagStr = btn.dataset.flags || 'g';
            
            // Reset flags
            Object.values(flags).forEach(cb => cb.checked = false);
            
            // Set flags from preset
            for (const char of flagStr) {
                if (char === 'g') flags.global.checked = true;
                if (char === 'i') flags.insensitive.checked = true;
                if (char === 'm') flags.multiline.checked = true;
                if (char === 's') flags.dotAll.checked = true;
                if (char === 'u') flags.unicode.checked = true;
                if (char === 'y') flags.sticky.checked = true;
            }
            
            updateResult();
        });
    });

    // Copy regex functionality
    btnCopyRegex.addEventListener('click', async () => {
        const pattern = regexPattern.value;
        if (!pattern) return;
        
        let flagStr = '';
        if (flags.global.checked) flagStr += 'g';
        if (flags.insensitive.checked) flagStr += 'i';
        if (flags.multiline.checked) flagStr += 'm';
        if (flags.dotAll.checked) flagStr += 's';
        if (flags.unicode.checked) flagStr += 'u';
        if (flags.sticky.checked) flagStr += 'y';
        
        const fullRegex = `/${pattern}/${flagStr}`;
        
        try {
            await navigator.clipboard.writeText(fullRegex);
            
            // Show feedback
            const originalIcon = btnCopyRegex.innerHTML;
            btnCopyRegex.innerHTML = '<i class="fas fa-check"></i>';
            setTimeout(() => {
                btnCopyRegex.innerHTML = originalIcon;
            }, 1500);
        } catch (err) {
            console.error('Failed to copy:', err);
        }
    });

    // Initial run
    updateResult();

    function syncScroll() {
        matchHighlights.scrollTop = testString.scrollTop;
        matchHighlights.scrollLeft = testString.scrollLeft;
    }

    function updateResult() {
        const pattern = regexPattern.value;
        const text = testString.value;
        
        // Handle empty input
        if (!text) {
            matchHighlights.innerHTML = '';
            matchInfo.textContent = '匹配结果：0 处匹配';
            regexError.style.display = 'none';
            regexPattern.classList.remove('error');
            return;
        }

        let flagStr = '';
        if (flags.global.checked) flagStr += 'g';
        if (flags.insensitive.checked) flagStr += 'i';
        if (flags.multiline.checked) flagStr += 'm';
        if (flags.dotAll.checked) flagStr += 's';
        if (flags.unicode.checked) flagStr += 'u';
        if (flags.sticky.checked) flagStr += 'y';

        try {
            const regex = new RegExp(pattern, flagStr);
            let matches = [];
            let match;
            
            // Safety check for empty pattern to avoid infinite loop
            if (pattern === '') {
                 // Do nothing for empty pattern
            } else if (!flags.global.checked) {
                match = regex.exec(text);
                if (match) {
                    matches.push({
                        start: match.index, 
                        end: match.index + match[0].length, 
                        text: match[0],
                        group: match.length > 1
                    });
                }
            } else {
                let lastIndex = 0;
                // Limit matches to prevent browser freeze on bad regex
                let matchCount = 0;
                const MAX_MATCHES = 2000; 

                while ((match = regex.exec(text)) !== null && matchCount < MAX_MATCHES) {
                    if (match[0].length === 0) {
                        regex.lastIndex++; // Avoid infinite loop for zero-length matches
                        continue;
                    }
                    matches.push({
                        start: match.index, 
                        end: match.index + match[0].length, 
                        text: match[0],
                        group: match.length > 1
                    });
                    matchCount++;
                }
            }
            
            renderHighlights(text, matches);
            matchInfo.textContent = `匹配结果：${matches.length} 处匹配`;
            regexError.style.display = 'none';
            regexPattern.classList.remove('error');
        } catch (e) {
            regexError.textContent = "正则表达式错误: " + e.message;
            regexError.style.display = 'block';
            regexPattern.classList.add('error');
            // On error, just show text without highlights
            matchHighlights.textContent = text;
        }
    }

    function escapeHtml(text) {
        return text
            .replace(/&/g, "&amp;")
            .replace(/</g, "&lt;")
            .replace(/>/g, "&gt;")
            .replace(/"/g, "&quot;")
            .replace(/'/g, "&#039;");
    }

    function renderHighlights(text, matches) {
        if (matches.length === 0) {
            matchHighlights.textContent = text; // Use textContent to safely render
            return;
        }

        let result = '';
        let lastIndex = 0;

        matches.forEach(match => {
            // Append text before match
            result += escapeHtml(text.substring(lastIndex, match.start));
            
            // Append match with highlight
            const matchText = escapeHtml(text.substring(match.start, match.end));
            const className = match.group ? 'match-highlight match-group' : 'match-highlight';
            result += `<span class="${className}">${matchText}</span>`;
            
            lastIndex = match.end;
        });

        // Append remaining text
        result += escapeHtml(text.substring(lastIndex));
        
        // Handle trailing newline for correct display
        if (result.endsWith('\n')) {
            result += '&nbsp;'; 
        }

        matchHighlights.innerHTML = result;
    }
});
