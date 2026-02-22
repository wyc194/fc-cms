document.addEventListener('DOMContentLoaded', function() {
    // 插件初始化
    dayjs.extend(window.dayjs_plugin_utc);
    dayjs.extend(window.dayjs_plugin_timezone);

    // 元素获取
    const nowTimeDisplay = document.getElementById('nowTimeDisplay');
    const nowTimestamp = document.getElementById('nowTimestamp');
    const btnPause = document.getElementById('btnPause');
    const globalTimezone = document.getElementById('globalTimezone');
    
    const inputTimestamp = document.getElementById('inputTimestamp');
    const btnTsToDate = document.getElementById('btnTsToDate');
    const outputDate = document.getElementById('outputDate');
    const tsUnitRadios = document.getElementsByName('tsUnit');

    const inputDate = document.getElementById('inputDate');
    const btnDateToTs = document.getElementById('btnDateToTs');
    const outputTimestampMs = document.getElementById('outputTimestampMs');
    const outputTimestampS = document.getElementById('outputTimestampS');

    let isPaused = false;
    let timerId = null;

    // 获取当前选择的时区
    function getSelectedTimezone() {
        return globalTimezone.value || dayjs.tz.guess();
    }

    // 更新当前时间显示
    function updateCurrentTime() {
        if (isPaused) return;
        
        const now = dayjs();
        const tz = getSelectedTimezone();
        
        // 显示格式化时间
        nowTimeDisplay.textContent = now.tz(tz).format('YYYY-MM-DD HH:mm:ss');
        
        // 显示时间戳
        nowTimestamp.textContent = now.valueOf();
    }

    // 启动定时器
    timerId = setInterval(updateCurrentTime, 1000);
    updateCurrentTime(); // 立即执行一次

    // 暂停/继续
    btnPause.addEventListener('click', () => {
        isPaused = !isPaused;
        const icon = btnPause.querySelector('i');
        if (isPaused) {
            icon.className = 'fas fa-play';
            btnPause.title = "继续";
        } else {
            icon.className = 'fas fa-pause';
            btnPause.title = "暂停";
            updateCurrentTime();
        }
    });

    // 时区切换
    globalTimezone.addEventListener('change', () => {
        updateCurrentTime();
        // 如果有输入值，尝试重新转换
        if (inputTimestamp.value) convertTsToDate();
        if (inputDate.value) convertDateToTs();
    });

    // 时间戳转日期
    function convertTsToDate() {
        const val = inputTimestamp.value.trim();
        if (!val) {
            outputDate.value = '';
            return;
        }

        let ts = parseInt(val, 10);
        if (isNaN(ts)) {
            outputDate.value = "无效的时间戳";
            return;
        }

        // 检查单位
        let unit = 'ms';
        for (const radio of tsUnitRadios) {
            if (radio.checked) {
                unit = radio.value;
                break;
            }
        }

        if (unit === 's') {
            ts *= 1000;
        }

        const date = dayjs(ts);
        if (!date.isValid()) {
            outputDate.value = "无效的时间戳";
            return;
        }

        const tz = getSelectedTimezone();
        outputDate.value = date.tz(tz).format('YYYY-MM-DD HH:mm:ss');
    }

    btnTsToDate.addEventListener('click', convertTsToDate);

    // 日期转时间戳
    function convertDateToTs() {
        const val = inputDate.value.trim();
        if (!val) {
            outputTimestampMs.value = '';
            outputTimestampS.value = '';
            return;
        }

        const tz = getSelectedTimezone();
        // 使用 dayjs.tz 解析特定时区的日期字符串
        const date = dayjs.tz(val, tz);

        if (!date.isValid()) {
            outputTimestampMs.value = "无效的日期格式";
            outputTimestampS.value = "";
            return;
        }

        const ms = date.valueOf();
        outputTimestampMs.value = ms;
        outputTimestampS.value = Math.floor(ms / 1000);
    }

    btnDateToTs.addEventListener('click', convertDateToTs);

    // 辅助功能：回车触发
    inputTimestamp.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') convertTsToDate();
    });
    inputDate.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') convertDateToTs();
    });

    // 默认填充当前时间到输入框方便测试
    const defaultTz = getSelectedTimezone();
    inputDate.value = dayjs().tz(defaultTz).format('YYYY-MM-DD HH:mm:ss');
    inputTimestamp.value = dayjs().valueOf();
});
