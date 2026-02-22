/**
 * main.js - 业务逻辑与 UI 交互
 * 将逻辑挂载到 btf 对象下，由 init.js 统一调度，增强 PJAX 兼容性
 */

(win => {
  const btf = win.btf

  /**
   * 基础 UI 初始化
   */
  btf.initUI = () => {
    // 移除 Valine 初始化，改为自研评论系统
    // const loadValine = () => { ... }

    const subtitleType = () => {
      if (!document.getElementById('subtitle')) return

      const runTyped = (content) => {
        const init = () => {
          new Typed('#subtitle', {
            strings: [content],
            startDelay: 300,
            typeSpeed: 150,
            loop: false,
            backSpeed: 50
          })
        }
        typeof Typed === 'function' ? init() : getScript(GLOBAL_CONFIG.source.typed).then(init)
      }

      const loadShiCi = () => {
        jinrishici.load(result => {
          runTyped(result?.data?.content || '长风破浪会有时，直挂云帆济沧海。')
        })
      }

      typeof jinrishici === 'object'
        ? loadShiCi()
        : getScript(GLOBAL_CONFIG.source.jinrishici)
          .then(loadShiCi)
          .catch(() => runTyped('长风破浪会有时，直挂云帆济沧海。'))
    }

    // if (document.getElementById('vcomment')) loadValine()
    if (document.getElementById('subtitle')) subtitleType()
    
    const $blogName = document.getElementById('site-name')
    let blogNameWidth = $blogName && $blogName.offsetWidth
    const $menusEle = document.querySelector('#menus .menus_items')
    let menusWidth = $menusEle && $menusEle.offsetWidth
    const $searchEle = document.querySelector('#search-button')
    let searchWidth = $searchEle && $searchEle.offsetWidth

    const adjustMenu = (change = false) => {
      const $nav = document.getElementById('nav')
      if (!$nav) return
      if (change) {
        blogNameWidth = $blogName && $blogName.offsetWidth
        menusWidth = $menusEle && $menusEle.offsetWidth
        searchWidth = $searchEle && $searchEle.offsetWidth
      }
      let t
      if (window.innerWidth < 768) t = true
      else t = (blogNameWidth || 0) + (menusWidth || 0) + (searchWidth || 0) > $nav.offsetWidth - 120

      if (t) {
        $nav.classList.add('hide-menu')
      } else {
        $nav.classList.remove('hide-menu')
      }
    }

    // sidebar menus
    const sidebarFn = () => {
      const $toggleMenu = document.getElementById('toggle-menu')
      const $mobileSidebarMenus = document.getElementById('sidebar-menus')
      const $menuMask = document.getElementById('menu-mask')
      const $body = document.body

      if (!$toggleMenu || !$mobileSidebarMenus || !$menuMask) return

      function openMobileSidebar () {
        btf.sidebarPaddingR()
        $body.style.overflow = 'hidden'
        btf.fadeIn($menuMask, 0.5)
        $mobileSidebarMenus.classList.add('open')
      }

      function closeMobileSidebar () {
        $body.style.overflow = ''
        $body.style.paddingRight = ''
        btf.fadeOut($menuMask, 0.5)
        $mobileSidebarMenus.classList.remove('open')
      }

      $toggleMenu.addEventListener('click', openMobileSidebar)

      $menuMask.addEventListener('click', e => {
        if ($mobileSidebarMenus.classList.contains('open')) {
          closeMobileSidebar()
        }
      })

      window.addEventListener('resize', e => {
        if (btf.isHidden($toggleMenu)) {
          if ($mobileSidebarMenus.classList.contains('open')) closeMobileSidebar()
        }
      })
    }

    /**
     * 首頁top_img底下的箭頭
     */
    const scrollDownInIndex = () => {
      const $scrollDownEle = document.getElementById('scroll-down')
      $scrollDownEle && $scrollDownEle.addEventListener('click', function () {
        btf.scrollToDest(document.getElementById('content-inner').offsetTop, 300)
      })
    }

    /**
     * 側邊欄sub-menu 展開/收縮
     */
    const clickFnOfSubMenu = function () {
      document.querySelectorAll('#sidebar-menus .expand').forEach(function (e) {
        e.addEventListener('click', function () {
          this.classList.toggle('hide')
          const $dom = this.parentNode.nextElementSibling
          if (btf.isHidden($dom)) {
            $dom.style.display = 'block'
          } else {
            $dom.style.display = 'none'
          }
        })
      })

      window.addEventListener('touchmove', function (e) {
        const $menusChild = document.querySelectorAll('#nav .menus_item_child')
        $menusChild.forEach(item => {
          if (!btf.isHidden(item)) item.style.display = 'none'
        })
      })
    }

    /**
     * 網頁運行時間与最后更新
     */
    const addRuntimeAndPushDate = () => {
      const $runtimeCount = document.getElementById('runtimeshow')
      if ($runtimeCount) {
        const publishDate = $runtimeCount.getAttribute('data-publishDate')
        $runtimeCount.innerText = btf.diffDate(publishDate) + ' ' + (GLOBAL_CONFIG.runtime || '')
      }
      const $lastPushDateItem = document.getElementById('last-push-date')
      if ($lastPushDateItem) {
        const lastPushDate = $lastPushDateItem.getAttribute('data-lastPushDate')
        $lastPushDateItem.innerText = btf.diffDate(lastPushDate, true)
      }
    }

    /**
     * 分类卡片展开/收缩
     */
    const toggleCardCategory = function () {
      const $cardCategory = document.querySelectorAll('#aside-cat-list .card-category-list-item.parent > .card-category-list-link')
      if ($cardCategory.length) {
        $cardCategory.forEach(function (item) {
          item.addEventListener('click', function (e) {
            const $target = e.target
            if ($target.classList.contains('fas') || $target.tagName.toLowerCase() === 'svg' || $target.closest('svg')) {
              e.preventDefault()
              const $icon = this.querySelector('i, svg')
              $icon.classList.toggle('expand')
              const $childUl = this.nextElementSibling
              if ($childUl) {
                if (btf.isHidden($childUl)) {
                  $childUl.style.display = 'block'
                } else {
                  $childUl.style.display = 'none'
                }
              }
            }
          })
        })
      }
    }

    /**
     * 评论切换
     */
    const switchComments = function () {
      let switchDone = false
      const $switchBtn = document.querySelector('#comment-switch > .switch-btn')
      $switchBtn && $switchBtn.addEventListener('click', function () {
        this.classList.toggle('move')
        document.querySelectorAll('#post-comment > .comment-wrap > div').forEach(function (item) {
          if (btf.isHidden(item)) {
            item.style.cssText = 'display: block;animation: tabshow .5s'
          } else {
            item.style.cssText = "display: none;animation: ''"
          }
        })

        if (!switchDone && typeof loadOtherComment === 'function') {
          switchDone = true
          loadOtherComment()
        }
      })
    }

    const lazyload = () => {
      if (typeof LazyLoad === 'function') {
        if (window.lazyLoadInstance) {
          window.lazyLoadInstance.update()
        } else {
          window.lazyLoadInstance = new LazyLoad({
             elements_selector: 'img',
             data_src: 'lazy-src'
           })
        }
      }
    }

    // 运行初始化
    adjustMenu()
    sidebarFn()
    scrollDownInIndex()
    clickFnOfSubMenu()
    addRuntimeAndPushDate()
    toggleCardCategory()
    switchComments()
    lazyload()
    
    const $nav = document.getElementById('nav')
    if ($nav) $nav.classList.add('show')

    // 暴露调整函数给 Rightside 使用
    btf.adjustMenu = adjustMenu
    // 暴露给 initArticle 使用
    btf.lazyload = lazyload
  }

  /**
   * 文章页初始化
   */
  btf.initArticle = () => {
    /**
     * 图片处理：增加懒加载支持
     */
    const processArticleImages = () => {
      const $article = document.getElementById('article-container')
      if (!$article) return
      
      const images = $article.querySelectorAll('img')
      const loadingImg = GLOBAL_CONFIG.loadingImg || '/img/loading.gif'
      
      images.forEach(img => {
        const src = img.getAttribute('src')
        if (src && !img.getAttribute('data-lazy-src') && !src.includes(loadingImg)) {
          img.setAttribute('data-lazy-src', src)
          img.setAttribute('src', loadingImg)
        }
      })
    }

    /**
     * 代码高亮工具
     */
    const addHighlightTool = function () {
      const highLight = GLOBAL_CONFIG.highlight
      if (!highLight) return

      // 初始化 highlight.js
      if (typeof hljs === 'object') {
        hljs.highlightAll()
      }

      const isHighlightCopy = highLight.highlightCopy
      const isHighlightLang = highLight.highlightLang
      const isHighlightShrink = GLOBAL_CONFIG_SITE.isHighlightShrink
      const highlightHeightLimit = highLight.highlightHeightLimit
      const isShowTool = isHighlightCopy || isHighlightLang || isHighlightShrink !== undefined
      
      // 兼容标准 markdown 的 pre code 结构
      const $figureHighlight = document.querySelectorAll('pre code')

      if (!((isShowTool || highlightHeightLimit) && $figureHighlight.length)) return

      const isPrismjs = highLight.plugin === 'prismjs'

      let highlightShrinkEle = ''
      let highlightCopyEle = ''
      const highlightShrinkClass = isHighlightShrink === true ? 'closed' : ''

      if (isHighlightShrink !== undefined) {
        highlightShrinkEle = `<i class="fas fa-angle-down expand ${highlightShrinkClass}"></i>`
      }

      if (isHighlightCopy) {
        highlightCopyEle = '<div class="copy-notice"></div><i class="fas fa-paste copy-button"></i>'
      }

      const copy = (text, ctx) => {
        if (navigator.clipboard && window.isSecureContext) {
          navigator.clipboard.writeText(text).then(() => {
            btf.snackbar.show(GLOBAL_CONFIG.copy.success, 'success')
            ctx && ctx.classList.replace('fa-paste', 'fa-check')
            setTimeout(() => {
              ctx && ctx.classList.replace('fa-check', 'fa-paste')
            }, 2000)
          })
        } else {
          const input = document.createElement('textarea')
          input.value = text
          document.body.appendChild(input)
          input.select()
          try {
            document.execCommand('copy')
            btf.snackbar.show(GLOBAL_CONFIG.copy.success, 'success')
            ctx && ctx.classList.replace('fa-paste', 'fa-check')
            setTimeout(() => {
              ctx && ctx.classList.replace('fa-check', 'fa-paste')
            }, 2000)
          } catch (err) {
            btf.snackbar.show(GLOBAL_CONFIG.copy.error, 'error')
          }
          document.body.removeChild(input)
        }
      }

      // click events
      const highlightCopyFn = (ele) => {
        const $nextEle = ele.nextElementSibling
        const $code = $nextEle.querySelector('.code')
        const text = $code ? $code.innerText : $nextEle.innerText
        copy(text, ele.lastChild)
      }

      const highlightShrinkFn = (ele) => {
        const $nextEle = ele.nextElementSibling
        ele.firstChild.classList.toggle('closed')
        if (btf.isHidden($nextEle)) {
          $nextEle.style.display = 'block'
        } else {
          $nextEle.style.display = 'none'
        }
      }

      const highlightToolsFn = function (e) {
        const $target = e.target.classList
        if ($target.contains('expand')) highlightShrinkFn(this)
        else if ($target.contains('copy-button')) highlightCopyFn(this)
      }

      function createEle (lang, item) {
        const fragment = document.createDocumentFragment()

        if (isShowTool) {
          const hlTools = document.createElement('div')
          hlTools.className = `highlight-tools ${highlightShrinkClass}`
          hlTools.innerHTML = highlightShrinkEle + lang + highlightCopyEle
          hlTools.addEventListener('click', highlightToolsFn)
          fragment.appendChild(hlTools)
        }

        const $parent = item.parentNode // pre
        
        // 增加行号逻辑
        const lines = item.innerText.replace(/\n$/, '').split('\n').length
        let lineNumbers = ''
        for (let i = 1; i <= lines; i++) {
          lineNumbers += `<span class="line-num">${i}</span>`
        }
        
        const highlightTable = `
          <table>
            <tr>
              <td class="gutter"><pre>${lineNumbers}</pre></td>
              <td class="code"><pre></pre></td>
            </tr>
          </table>
        `
        const tempDiv = document.createElement('div')
        tempDiv.innerHTML = highlightTable
        const $table = tempDiv.querySelector('table')
        $table.querySelector('.code pre').appendChild(item)
        
        $parent.innerHTML = ''
        $parent.appendChild(fragment)
        $parent.appendChild($table)
        $parent.classList.add('highlight')
        if (isHighlightShrink === true) $table.style.display = 'none'
      }

      $figureHighlight.forEach(function (item) {
        const $pre = item.parentNode
        if ($pre.tagName !== 'PRE') return

        let langName = 'Code'
        const classes = item.getAttribute('class')
        if (classes) {
          const match = classes.match(/language-(\w+)/)
          if (match) langName = match[1].toUpperCase()
        }

        const highlightLangEle = isHighlightLang ? `<div class="code-lang">${langName}</div>` : ''
        createEle(highlightLangEle, item)
      })
    }

    /**
     * 图片描述与图库
     */
    const addPhotoFigcaption = () => {
      document.querySelectorAll('#article-container img').forEach(function (item) {
        const parentEle = item.parentNode
        if (!parentEle.parentNode.classList.contains('justified-gallery')) {
          const ele = document.createElement('div')
          ele.className = 'img-alt is-center'
          ele.textContent = item.getAttribute('alt')
          parentEle.insertBefore(ele, item.nextSibling)
        }
      })
    }

    const runJustifiedGallery = function (ele) {
      const $justifiedGallery = $(ele)
      const $imgList = $justifiedGallery.find('img')
      $imgList.unwrap()
      if ($imgList.length) {
        $imgList.each(function (i, o) {
          if ($(o).attr('data-lazy-src')) $(o).attr('src', $(o).attr('data-lazy-src'))
          $(o).wrap('<div></div>')
        })
      }

      if (typeof $.fn.justifiedGallery !== 'undefined') {
        btf.initJustifiedGallery($justifiedGallery)
      } else {
        getCSS(GLOBAL_CONFIG.source.justifiedGallery.css).then(() => {
          getScript(GLOBAL_CONFIG.source.justifiedGallery.js).then(() => {
            btf.initJustifiedGallery($justifiedGallery)
          })
        })
      }
    }

    const addFancybox = function (ele) {
      const runFancybox = (ele) => {
        ele.each(function (i, o) {
          const $this = $(o)
          const lazyloadSrc = $this.attr('data-lazy-src') || $this.attr('src')
          const dataCaption = $this.attr('alt') || ''
          $this.wrap(`<a href="${lazyloadSrc}" data-fancybox="group" data-caption="${dataCaption}" class="fancybox"></a>`)
        })

        $().fancybox({
          selector: '[data-fancybox]',
          loop: true,
          transitionEffect: 'slide',
          protect: true,
          buttons: ['slideShow', 'fullScreen', 'thumbs', 'close'],
          hash: false
        })
      }

      if (typeof $.fancybox === 'undefined') {
        getCSS(GLOBAL_CONFIG.source.fancybox.css).then(() => {
          getScript(GLOBAL_CONFIG.source.fancybox.js).then(() => {
            runFancybox($(ele))
          })
        })
      } else {
        runFancybox($(ele))
      }
    }

    /**
     * TOC 目录 (使用 tocbot)
     */
    const tocFn = function () {
      const $cardTocLayout = document.getElementById('card-toc')
      if (!$cardTocLayout) return
      
      const $tocContainer = document.getElementById('toc-container')
      if (!$tocContainer) return

      const initTocbot = () => {
        tocbot.init({
          tocSelector: '#toc-container', 
          contentSelector: '#article-container',
          headingSelector: 'h1, h2, h3, h4, h5, h6',
          hasInnerContainers: true,
          scrollSmooth: true,
          scrollSmoothDuration: 420,
          headingsOffset: 70, 
          scrollSmoothOffset: -70,
          collapseDepth: 6,
          orderedList: false
        })
      }

      if (typeof tocbot === 'object') {
        initTocbot()
      } else {
        getCSS(GLOBAL_CONFIG.source.tocbot.css)
        getScript(GLOBAL_CONFIG.source.tocbot.js).then(initTocbot)
      }
    }

    /**
     * 初始化评论系统
     */
    btf.initCommentSystem = () => {
      const $commentForm = document.getElementById('comment-form')
      const $commentList = document.getElementById('comment-list')
      if (!$commentForm || !$commentList) return

      const $submitBtn = document.getElementById('submit-comment')
      const $nickname = document.getElementById('comment-nickname')
      const $email = document.getElementById('comment-email')
      const $content = document.getElementById('comment-content')
      const $verificationCode = document.getElementById('comment-verification-code')
      const $sendCodeBtn = document.getElementById('send-code-btn')
      const $articleId = document.getElementById('comment-article-id')
      const $parentId = document.getElementById('comment-parent-id')

      const articleId = $articleId.value
      let page = 1 // 后端 Spring Data JPA 分页配置了从 1 开始
      let hasMore = true
      let isLoading = false
      const size = 5

      // 从本地存储读取用户信息
      const loadUserInfo = () => {
        const userInfo = btf.optLocal.get('comment-user-info')
        if (userInfo) {
          if ($nickname) $nickname.value = userInfo.nickname || ''
          if ($email) $email.value = userInfo.email || ''
        }
      }

      // 保存用户信息到本地存储
      const saveUserInfo = (nickname, email) => {
        btf.optLocal.set('comment-user-info', { nickname, email }, 365)
      }

      loadUserInfo()

      /**
       * 格式化时间
       */
      const formatTime = (timeStr) => {
        const date = new Date(timeStr)
        const y = date.getFullYear()
        const m = String(date.getMonth() + 1).padStart(2, '0')
        const d = String(date.getDate()).padStart(2, '0')
        const hh = String(date.getHours()).padStart(2, '0')
        const mm = String(date.getMinutes()).padStart(2, '0')
        return `${y}-${m}-${d} ${hh}:${mm}`
      }

      /**
       * 渲染单个评论项
       */
      const renderCommentItem = (comment, depth = 1) => {
        const avatar = btf.getLetterAvatar(comment.nickname)
        const isReply = depth > 1
        
        // 构建回复按钮逻辑：
        // 1. 一级评论可以回复
        // 2. 二级回复可以回复
        // 3. 三级及以上回复，点击回复时 parentId 统一设为三级回复的父级（即二级回复），
        //    或者简单处理：三级评论不再显示回复按钮，或回复按钮依然绑定当前ID但UI不再深层嵌套
        // 根据需求：三层限制。我们让三级评论的回复依然挂在三级评论下，但在渲染时通过 depth 控制不再缩进。
        const showReplyBtn = depth < 3 

        return `
          <div class="comment-item ${isReply ? 'reply-item' : ''}" id="comment-${comment.id}">
            <div class="comment-avatar">
              <img src="${avatar}" alt="${comment.nickname}">
            </div>
            <div class="comment-main">
              <div class="comment-info">
                <span class="comment-nickname">${comment.nickname}</span>
                ${comment.adminReply ? '<span class="comment-admin-tag">博主</span>' : ''}
                <time class="comment-time" datetime="${comment.createTime}">${formatTime(comment.createTime)}</time>
                ${showReplyBtn ? `<a class="comment-reply-btn" href="javascript:void(0)" onclick="replyComment(${comment.id}, '${comment.nickname}')">回复</a>` : ''}
              </div>
              <div class="comment-content">${isReply && depth > 2 ? `<span class="reply-to">@${comment.parentNickname || '未知'}</span> ` : ''}${comment.content}</div>
              ${comment.replies && comment.replies.length > 0 ? `
                <div class="comment-replies" style="${depth >= 2 ? 'margin-left: 0; border-left: none; padding-left: 0;' : ''}">
                  ${Array.from(comment.replies).map(reply => renderCommentItem(reply, depth + 1)).join('')}
                </div>
              ` : ''}
            </div>
          </div>
        `
      }

      /**
       * 加载评论
       */
      const loadComments = (isFirst = false, scrollToId = null) => {
        if (isLoading || (!hasMore && !isFirst)) return
        isLoading = true

        if (isFirst) {
          page = 1
          hasMore = true
          $commentList.innerHTML = '<div class="comment-loading text-center"><i class="fas fa-spinner fa-spin"></i> 正在加载评论...</div>'
        } else {
          const loadingMore = document.createElement('div')
          loadingMore.className = 'comment-loading-more text-center'
          loadingMore.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 正在加载更多...'
          $commentList.appendChild(loadingMore)
        }

        fetch(`/api/comments/article/${articleId}?page=${page}&size=${size}`)
          .then(response => response.json())
          .then(res => {
            if (res.code === 200) {
              const data = res.data
              const comments = data.content
              
              if (isFirst) {
                $commentList.innerHTML = ''
                if (comments.length === 0) {
                  $commentList.innerHTML = '<div class="comment-none">暂无评论，快来抢沙发吧~</div>'
                  hasMore = false
                  return
                }
              } else {
                const loadingMore = $commentList.querySelector('.comment-loading-more')
                if (loadingMore) loadingMore.remove()
              }

              const html = comments.map(comment => renderCommentItem(comment)).join('')
              $commentList.insertAdjacentHTML('beforeend', html)

              page++
              hasMore = !data.last
              
              // 移除旧的 sentinel 和 “没有更多” 提示，确保它们始终在最底部
              const oldSentinel = document.getElementById('comment-pagination-sentinel')
              if (oldSentinel) oldSentinel.remove()
              const oldNoMore = $commentList.querySelector('.comment-no-more')
              if (oldNoMore) oldNoMore.remove()

              if (!hasMore && comments.length > 0) {
                $commentList.insertAdjacentHTML('beforeend', '<div class="comment-no-more">没有更多评论了</div>')
              }
              
              // 确保观察者在有更多数据时继续工作，且 sentinel 始终在末尾
              if (hasMore) {
                const sentinel = document.createElement('div')
                sentinel.id = 'comment-pagination-sentinel'
                sentinel.style.height = '1px' // 1px 足够触发，且不占位
                $commentList.appendChild(sentinel)
                observer.observe(sentinel)
              }

              // 如果需要滚动到指定评论
              if (scrollToId) {
                setTimeout(() => {
                  const $newComment = document.getElementById(`comment-${scrollToId}`)
                  if ($newComment) {
                    // 1. 先让评论项高亮
                    $newComment.classList.add('comment-highlight')
                    
                    // 2. 滚动到评论项
                    // 使用 nearest 确保它在容器内可见，不强制置中以减少剧烈跳动
                    $newComment.scrollIntoView({ behavior: 'smooth', block: 'nearest' })
                    
                    // 3. 额外处理：如果是移动端或评论框被遮挡，可能需要滚动整个窗口
                    const rect = $newComment.getBoundingClientRect()
                    if (rect.top < 0 || rect.bottom > window.innerHeight) {
                      $newComment.scrollIntoView({ behavior: 'smooth', block: 'center' })
                    }
                  }
                }, 300)
              }
            }
          })
          .catch(err => {
            console.error('加载评论出错:', err)
          })
          .finally(() => {
            isLoading = false
          })
      }

      // IntersectionObserver 监听滚动 (改为监听容器内部滚动)
      const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting && hasMore && !isLoading) {
          loadComments()
        }
      }, { 
        root: $commentList, 
        rootMargin: '20px' 
      })

      // 初始加载
      loadComments(true)

      // 回复功能：将 parentId 设置到隐藏域，并自动填充回复提示
      window.replyComment = (id, nickname) => {
        $parentId.value = id
        $content.placeholder = `回复 @${nickname}: `
        $content.focus()
        // 滚动到表单
        $commentForm.scrollIntoView({ behavior: 'smooth' })
      }

      /**
       * 发送验证码逻辑
       */
      $sendCodeBtn.addEventListener('click', async () => {
        const email = $email.value.trim()
        if (!email) {
          btf.snackbar.show('请输入邮箱', 'warning')
          return
        }
        if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
          btf.snackbar.show('邮箱格式不正确', 'warning')
          return
        }

        try {
          $sendCodeBtn.disabled = true
          const response = await fetch('/api/comments/verification-code', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email })
          })
          const result = await response.json()
          if (result.code === 200) {
            btf.snackbar.show('验证码已发送至您的邮箱，请查收', 'success')
            let countdown = 60
            const timer = setInterval(() => {
              $sendCodeBtn.innerText = `${countdown}s`
              countdown--
              if (countdown < 0) {
                clearInterval(timer)
                $sendCodeBtn.disabled = false
                $sendCodeBtn.innerText = '获取验证码'
              }
            }, 1000)
          } else {
            btf.snackbar.show(result.message || '发送失败', 'error')
            $sendCodeBtn.disabled = false
          }
        } catch (error) {
          console.error('Send verification code error:', error)
          btf.snackbar.show('发送失败，请稍后再试', 'error')
          $sendCodeBtn.disabled = false
        }
      })

      /**
       * 提交评论逻辑
       */
      $submitBtn.addEventListener('click', async () => {
        const nickname = $nickname.value.trim()
        const email = $email.value.trim()
        const content = $content.value.trim()
        const verificationCode = $verificationCode.value.trim()
        const parentId = $parentId.value

        if (!nickname || !email || !content || !verificationCode) {
          btf.snackbar.show('请填写必填项（昵称、邮箱、内容、验证码）', 'warning')
          return
        }

        try {
          $submitBtn.disabled = true
          $submitBtn.textContent = '提交中...'
          const response = await fetch('/api/comments', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({
              articleId,
              nickname,
              email,
              content,
              verificationCode,
              parentId: parentId || null
            })
          })

          const result = await response.json()
          if (result.code === 200) {
            btf.snackbar.show('评论提交成功，审核通过后将显示', 'success')
            saveUserInfo(nickname, email)
            // 清空表单
            $content.value = ''
            $verificationCode.value = ''
            $parentId.value = ''
            $content.placeholder = '欢迎交流讨论... *'
            // 重新加载评论列表
            setTimeout(() => {
              loadComments(true, result.data.id)
            }, 500)
          } else {
            btf.snackbar.show(result.message || '提交失败', 'error')
          }
        } catch (error) {
          console.error('Submit comment error:', error)
          btf.snackbar.show('网络错误，请稍后再试', 'error')
        } finally {
          $submitBtn.disabled = false
          $submitBtn.textContent = '提交评论'
        }
      })
    }

    /**
     * 填充文章链接
     */
    const addPostUrl = () => {
      const postUrlElem = document.getElementById('post-url')
      if (postUrlElem) {
        postUrlElem.textContent = window.location.href
        postUrlElem.href = window.location.href
      }
    }

    /**
     * 分享组件初始化
     */
    const initShare = () => {
      const $share = document.querySelector('.social-share')
      if ($share) {
        if (typeof socialShare === 'object') {
          // 已经加载
        } else {
          getCSS(GLOBAL_CONFIG.source.socialShare.css)
          getScript(GLOBAL_CONFIG.source.socialShare.js)
        }
      }
    }

    /**
     * 版权信息与表格处理
     */
    const addCopyright = () => {
      const copyright = GLOBAL_CONFIG.copyright
      if (!copyright) return
      document.body.oncopy = (e) => {
        e.preventDefault()
        let textFont; const copyFont = window.getSelection(0).toString()
        if (copyFont.length > copyright.limitCount) {
          textFont = copyFont + '\n\n' +
              copyright.languages.author + '\n' +
              copyright.languages.link + window.location.href + '\n' +
              copyright.languages.source + '\n' +
              copyright.languages.info
        } else {
          textFont = copyFont
        }
        if (e.clipboardData) return e.clipboardData.setData('text', textFont)
        else return window.clipboardData.setData('text', textFont)
      }
    }

    const addTableWrap = () => {
      const $table = document.querySelectorAll('#article-container :not(.highlight) > table, #article-container > table')
      $table.forEach(item => btf.wrap(item, 'div', '', 'table-wrap'))
    }

    const clickFnOfTagHide = function () {
      const $hideInline = document.querySelectorAll('#article-container .hide-button')
      if ($hideInline.length) {
        $hideInline.forEach(function (item) {
          item.addEventListener('click', function (e) {
            const $this = this
            const $hideContent = $this.nextElementSibling
            $this.classList.toggle('open')
            if ($this.classList.contains('open')) {
              if ($hideContent.querySelectorAll('.justified-gallery').length > 0) {
                btf.initJustifiedGallery($hideContent.querySelectorAll('.justified-gallery'))
              }
            }
          })
        })
      }
    }

    const tabsFn = {
      clickFnOfTabs: function () {
        document.querySelectorAll('#article-container .tab > button').forEach(function (item) {
          item.addEventListener('click', function (e) {
            const $this = this
            const $tabItem = $this.parentNode

            if (!$tabItem.classList.contains('active')) {
              const $tabContent = $tabItem.parentNode.nextElementSibling
              const $siblings = btf.siblings($tabItem, '.active')[0]
              $siblings && $siblings.classList.remove('active')
              $tabItem.classList.add('active')
              const tabId = $this.getAttribute('data-href').replace('#', '')
              const childList = [...$tabContent.children]
              childList.forEach(item => {
                if (item.id === tabId) item.classList.add('active')
                else item.classList.remove('active')
              })
              const $isTabJustifiedGallery = $tabContent.querySelectorAll(`#${tabId} .justified-gallery`)
              if ($isTabJustifiedGallery.length > 0) {
                btf.initJustifiedGallery($isTabJustifiedGallery)
              }
            }
          })
        })
      },
      backToTop: () => {
        document.querySelectorAll('#article-container .tabs .tab-to-top').forEach(function (item) {
          item.addEventListener('click', function () {
            btf.scrollToDest(btf.getEleTop(btf.getParents(this, '.tabs')), 300)
          })
        })
      }
    }

    const addPostOutdateNotice = function () {
      const data = GLOBAL_CONFIG.noticeOutdate
      if (!data) return
      const diffDay = btf.diffDate(GLOBAL_CONFIG_SITE.postUpdate)
      if (diffDay >= data.limitDay) {
        const ele = document.createElement('div')
        ele.className = 'post-outdate-notice'
        ele.innerHTML = `<div class="error-no-27"><i class="fas fa-exclamation-circle"></i> ${data.messagePrev} ${diffDay} ${data.messageNext}</div>`
        const $article = document.getElementById('article-container')
        if ($article) $article.insertBefore(ele, $article.firstChild)
      }
    }

    // 初始化自研评论系统
    // 检查是否在文章详情页
    if (document.getElementById('post-comment')) {
      btf.initCommentSystem()
    }

    addHighlightTool()
    addPhotoFigcaption()
    addCopyright()
    addTableWrap()
    tocFn()
    addPostUrl()
    initShare()
    clickFnOfTagHide()
    tabsFn.clickFnOfTabs()
    tabsFn.backToTop()
    addPostOutdateNotice()

    // JQuery 依赖项
    const $fancyboxEle = GLOBAL_CONFIG.lightbox === 'fancybox'
        ? document.querySelectorAll('#article-container :not(a):not(.gallery-group) > img:not(.no-fancybox), #article-container > img:not(.no-fancybox)')
        : []
    const $jgEle = document.querySelectorAll('#article-container .justified-gallery')

    processArticleImages() // 在 fancybox 和 lazyload 运行前处理图片
    btf.lazyload() // 重新触发懒加载以识别新添加的 data-lazy-src
    
    if ($jgEle.length > 0 || $fancyboxEle.length > 0) {
      btf.isJqueryLoad(() => {
        $jgEle.length > 0 && runJustifiedGallery($jgEle)
        $fancyboxEle.length > 0 && addFancybox($fancyboxEle)
      })
    }
  }

  // 移除旧的事件监听，改为在 init.js 中调度
  // document.addEventListener('DOMContentLoaded', ...)

})(window)

