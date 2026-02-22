/**
 * init.js - 页面初始化核心脚本
 * 包含：本地存储封装、脚本异步加载、主题切换初始化、侧边栏状态初始化
 * 以及从 common.js 合并而来的设置条控制和滚动逻辑
 */

(win => {
    // --- 1. 立即执行的初始化逻辑 (阻塞式防止闪屏) ---

    // 兼容旧代码，使用 utils.js 中定义的统一存储工具
    win.optLocal = btf.optLocal

    win.getScript = url => new Promise((resolve, reject) => {
        if (document.querySelector(`script[src="${url}"]`)) return resolve()
        const script = document.createElement('script')
        script.src = url
        script.async = true
        script.onerror = reject
        script.onload = script.onreadystatechange = function() {
            const loadState = this.readyState
            if (loadState && loadState !== 'loaded' && loadState !== 'complete') return
            script.onload = script.onreadystatechange = null
            resolve()
        }
        document.head.appendChild(script)
    })

    win.getCSS = (url, id) => new Promise((resolve, reject) => {
        if (id && document.getElementById(id)) return resolve()
        const link = document.createElement('link')
        link.rel = 'stylesheet'
        link.type = 'text/css'
        link.href = url
        if (id) link.id = id
        link.onerror = reject
        link.onload = resolve
        document.head.appendChild(link)
    })

    win.activateDarkMode = function () {
        document.documentElement.setAttribute('data-theme', 'dark')
        if (document.querySelector('meta[name="theme-color"]') !== null) {
            document.querySelector('meta[name="theme-color"]').setAttribute('content', '#0d0d0d')
        }
    }
    win.activateLightMode = function () {
        document.documentElement.setAttribute('data-theme', 'light')
        if (document.querySelector('meta[name="theme-color"]') !== null) {
            document.querySelector('meta[name="theme-color"]').setAttribute('content', '#ffffff')
        }
    }

    const theme = optLocal.get('theme')
    const adminTheme = (window.GLOBAL_CONFIG && window.GLOBAL_CONFIG.themeSetting) || 'auto'

    const getSystemTheme = () => {
        const isDarkMode = window.matchMedia('(prefers-color-scheme: dark)').matches
        const isLightMode = window.matchMedia('(prefers-color-scheme: light)').matches
        if (isDarkMode) return 'dark'
        if (isLightMode) return 'light'
        
        // 兜底：根据时间段判断
        const hour = new Date().getHours()
        return (hour <= 6 || hour >= 18) ? 'dark' : 'light'
    }

    const initTheme = () => {
        // 1. 确定最终要应用的主题模式
        let targetMode = theme
        if (!targetMode) {
            targetMode = adminTheme === 'auto' ? getSystemTheme() : adminTheme
        }

        // 2. 执行激活
        targetMode === 'dark' ? activateDarkMode() : activateLightMode()

        // 3. 注册自动联动监听 (仅在未手动选择且设置为 auto 时生效)
        if (!theme && adminTheme === 'auto') {
            window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', e => {
                if (!optLocal.get('theme')) {
                    e.matches ? activateDarkMode() : activateLightMode()
                }
            })
        }
    }

    initTheme()

    const asideStatus = optLocal.get('aside-status')
    if (asideStatus !== undefined) {
        if (asideStatus === 'hide') {
            document.documentElement.classList.add('hide-aside')
        } else {
            document.documentElement.classList.remove('hide-aside')
        }
    }

    // --- 2. 工具函数 (定义核心命名空间，其余工具函数由 utils.js 提供) ---
    win.btf = {
        ...(win.btf || {}),
    }

    // --- 3. 页面初始化与重初始化机制 ---

    /**
     * 初始化设置条与滚动逻辑
     */
    btf.initRightside = () => {
        if (document.getElementById('rightside')) return

        const isToolPage = window.location.pathname.includes('/tools') || !!document.getElementById('tool-page')
        const showHideAside = !isToolPage
        const html = `
            <div id="rightside">
                <div id="rightside-config-hide">
                    <button id="translateLink" type="button" title="简繁转换">繁</button>
                    <button id="darkmode" type="button" title="浅色和深色模式转换"><i class="fas fa-adjust"></i></button>
                    ${showHideAside ? `
                    <button id="hide-aside-btn" type="button" title="单栏和双栏切换">
                        <i class="fas fa-arrows-alt-h"></i>
                    </button>` : ''}
                </div>
                <div id="rightside-config-show">
                    <button id="rightside_config" type="button" title="设置"><i class="fas fa-cog fa-spin"></i></button>
                    <button id="go-up" type="button" title="回到顶部"><i class="fas fa-arrow-up"></i></button>
                </div>
            </div>
        `
        document.body.insertAdjacentHTML('beforeend', html)

        const $rightside = document.getElementById('rightside')

        const rightSideFn = {
            switchDarkMode: () => {
                const nowMode = document.documentElement.getAttribute('data-theme') === 'dark' ? 'dark' : 'light'
                if (nowMode === 'light') {
                    activateDarkMode()
                    optLocal.set('theme', 'dark', 2)
                    GLOBAL_CONFIG.Snackbar !== undefined && btf.snackbar.show(GLOBAL_CONFIG.Snackbar.day_to_night)
                } else {
                    activateLightMode()
                    optLocal.set('theme', 'light', 2)
                    GLOBAL_CONFIG.Snackbar !== undefined && btf.snackbar.show(GLOBAL_CONFIG.Snackbar.night_to_day)
                }
                typeof utterancesTheme === 'function' && utterancesTheme()
            },
            showOrHideBtn: () => {
                const $configHide = document.getElementById('rightside-config-hide')
                $configHide && $configHide.classList.toggle('show')
            },
            scrollToTop: () => btf.scrollToDest(0, 500),
            hideAsideBtn: () => {
                const $htmlDom = document.documentElement.classList
                $htmlDom.contains('hide-aside')
                    ? optLocal.set('aside-status', 'show', 2)
                    : optLocal.set('aside-status', 'hide', 2)
                $htmlDom.toggle('hide-aside')
            }
        }

        $rightside.addEventListener('click', (e) => {
            const $target = e.target.id || e.target.parentNode.id
            switch ($target) {
                case 'go-up': rightSideFn.scrollToTop(); break
                case 'rightside_config': rightSideFn.showOrHideBtn(); break
                case 'darkmode': rightSideFn.switchDarkMode(); break
                case 'hide-aside-btn': rightSideFn.hideAsideBtn(); break
            }
        })

        let initTop = 0
        const $header = document.getElementById('page-header')
        const scrollFn = btf.throttle(() => {
            const currentTop = window.scrollY || document.documentElement.scrollTop
            const isDown = currentTop > initTop
            initTop = currentTop

            // Header 逻辑
            if ($header) {
                if (currentTop > 56) {
                    $header.classList.add('nav-fixed')
                    if (isDown) {
                        $header.classList.contains('nav-visible') && $header.classList.remove('nav-visible')
                    } else {
                        !$header.classList.contains('nav-visible') && $header.classList.add('nav-visible')
                    }
                } else {
                    if (currentTop === 0) {
                        $header.classList.remove('nav-fixed', 'nav-visible')
                    }
                }
            }

            // Rightside 逻辑: 只要超过50px就显示，不再跟随滚动方向隐藏
            if (currentTop > 50) {
                $rightside.style.opacity = '1'
                $rightside.style.transform = 'translateX(-38px)'
            } else {
                $rightside.style.opacity = ''
                $rightside.style.transform = ''
            }
        }, 200)

        window.addEventListener('scroll', scrollFn)
        scrollFn()
    }

    /**
     * 加载全局基础资源 (Snackbar, Lazyload, Ribbon 等)
     */
    btf.loadGlobalResources = () => {
        const { snackbar, lazyload, ribbon, clickHeart } = GLOBAL_CONFIG.source
        
        // 关键资源：Lazyload 立即加载
        if (lazyload) {
            getScript(lazyload).then(() => {
                if (window.lazyLoadInstance) {
                    window.lazyLoadInstance.update()
                } else if (typeof LazyLoad === 'function') {
                    window.lazyLoadInstance = new LazyLoad({
                        elements_selector: 'img',
                        data_src: 'lazy-src'
                    })
                }
            })
        }
        
        // 非关键资源：延迟加载以提高首屏速度
        setTimeout(() => {
            if (snackbar) {
                getCSS(snackbar.css, 'node-snackbar-css')
                getScript(snackbar.js)
            }
            if (ribbon) getScript(ribbon)
            if (clickHeart) getScript(clickHeart)
        }, 2000) // 延迟 2 秒加载背景特效和通知组件
    }

    // 立即启动关键资源加载
    btf.loadGlobalResources()

    /**
     * 统一初始化函数
     */
    btf.init = () => {
        btf.initRightside()
        
        // 执行 main.js 中定义的 UI 初始化
        if (typeof btf.initUI === 'function') {
            btf.initUI()
        }
        
        // 如果是文章页，执行文章相关初始化
        if (document.getElementById('article-container')) {
            if (typeof btf.initArticle === 'function') {
                btf.initArticle()
            }
        }
    }

    // --- 4. 事件监听 ---

    document.addEventListener('DOMContentLoaded', btf.init)
    document.addEventListener('pjax:complete', btf.init)

})(window)
