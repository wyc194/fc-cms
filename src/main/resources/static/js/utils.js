window.btf = {
  ...window.btf,

  debounce: (func, wait, immediate) => {
    let timeout
    return function () {
      const context = this
      const args = arguments
      const later = function () {
        timeout = null
        if (!immediate) func.apply(context, args)
      }
      const callNow = immediate && !timeout
      clearTimeout(timeout)
      timeout = setTimeout(later, wait)
      if (callNow) func.apply(context, args)
    }
  },

  throttle: function (func, wait, options) {
    let timeout, context, args
    let previous = 0
    if (!options) options = {}

    const later = function () {
      previous = options.leading === false ? 0 : Date.now()
      timeout = null
      func.apply(context, args)
      if (!timeout) context = args = null
    }

    return function () {
      const now = Date.now()
      if (!previous && options.leading === false) previous = now
      const remaining = wait - (now - previous)
      context = this
      args = arguments
      if (remaining <= 0 || remaining > wait) {
        if (timeout) {
          clearTimeout(timeout)
          timeout = null
        }
        previous = now
        func.apply(context, args)
        if (!timeout) context = args = null
      } else if (!timeout && options.trailing !== false) {
        timeout = setTimeout(later, remaining)
      }
    }
  },

  sidebarPaddingR: () => {
    const innerWidth = window.innerWidth
    const clientWidth = document.body.clientWidth
    const paddingRight = innerWidth - clientWidth
    if (innerWidth !== clientWidth) {
      document.body.style.paddingRight = paddingRight + 'px'
    }
  },

  scrollToDest: (pos, time = 500) => {
    const currentPos = window.pageYOffset
    const isHigher = currentPos > pos
    let start = null
    const distance = isHigher ? currentPos - pos : pos - currentPos

    if (distance <= 0) return

    window.requestAnimationFrame(function step (timestamp) {
      if (!start) start = timestamp
      const progress = timestamp - start
      const percentage = Math.min(progress / time, 1)
      const offset = distance * (percentage * (2 - percentage))
      window.scrollTo(0, isHigher ? currentPos - offset : currentPos + offset)
      if (progress < time) {
        window.requestAnimationFrame(step)
      } else {
        window.scrollTo(0, pos)
      }
    })
  },

  snackbar: {
    show: (text, typeOrAction = 'default', duration = 2000) => {
      if (typeof Snackbar !== 'undefined') {
        const isAction = typeof typeOrAction === 'boolean'
        const type = isAction ? 'default' : typeOrAction
        const showAction = isAction ? typeOrAction : false

        let backgroundColor = ''
        switch (type) {
          case 'success': backgroundColor = '#52c41a'; break
          case 'error': backgroundColor = '#ff4d4f'; break
          case 'warning': backgroundColor = '#faad14'; break
          case 'info': backgroundColor = '#1890ff'; break
          default: backgroundColor = document.documentElement.getAttribute('data-theme') === 'light' 
            ? (GLOBAL_CONFIG.Snackbar && GLOBAL_CONFIG.Snackbar.bgLight) 
            : (GLOBAL_CONFIG.Snackbar && GLOBAL_CONFIG.Snackbar.bgDark)
        }

        Snackbar.show({
          text: text,
          showAction: showAction,
          duration: duration,
          pos: (GLOBAL_CONFIG.Snackbar && GLOBAL_CONFIG.Snackbar.position) || 'bottom-left',
          backgroundColor: backgroundColor
        })
      } else {
        alert(text)
      }
    }
  },

  // 兼容旧版调用
  snackbarShow: (text, showAction = false, duration = 2000) => {
    btf.snackbar.show(text, showAction, duration)
  },

  fadeIn: (ele, time) => {
    ele.style.cssText = `display:block;animation: to_show ${time}s`
  },

  fadeOut: (ele, time) => {
    ele.addEventListener('animationend', function f() {
      ele.style.cssText = "display: none; animation: '' "
      ele.removeEventListener('animationend', f)
    })
    ele.style.animation = `to_hide ${time}s`
  },

  isHidden: (ele) => ele.offsetHeight === 0 && ele.offsetWidth === 0,

  getParents: (elem, selector) => {
    for (; elem && elem !== document; elem = elem.parentNode) {
      if (elem.matches(selector)) return elem
    }
    return null
  },

  siblings: (ele, selector) => {
    return [...ele.parentNode.children].filter((child) => {
      if (selector) {
        return child !== ele && child.matches(selector)
      }
      return child !== ele
    })
  },

  wrap: function (selector, eleType, id = '', cn = '') {
    const creatEle = document.createElement(eleType)
    if (id) creatEle.id = id
    if (cn) creatEle.className = cn
    selector.parentNode.insertBefore(creatEle, selector)
    creatEle.appendChild(selector)
  },

  unwrap: function (el) {
    const elParentNode = el.parentNode
    if (elParentNode !== document.body) {
      elParentNode.parentNode.insertBefore(el, elParentNode)
      elParentNode.parentNode.removeChild(elParentNode)
    }
  },

  getEleTop: (ele) => {
    let actualTop = ele.offsetTop
    let current = ele.offsetParent

    while (current !== null) {
      actualTop += current.offsetTop
      current = current.offsetParent
    }

    return actualTop
  },

  isJqueryLoad: (fn) => {
    if (typeof jQuery === 'undefined') {
      getScript(GLOBAL_CONFIG.source.jQuery).then(fn)
    } else {
      fn()
    }
  },

  diffDate: (d, more = false) => {
    const dateNow = new Date()
    const datePost = new Date(d)
    const dateDiff = dateNow.getTime() - datePost.getTime()
    const minute = 1000 * 60
    const hour = minute * 60
    const day = hour * 24
    const month = day * 30

    let result
    if (more) {
      const monthCount = dateDiff / month
      const dayCount = dateDiff / day
      const hourCount = dateDiff / hour
      const minuteCount = dateDiff / minute

      if (monthCount >= 1) result = datePost.toLocaleDateString().replace(/\//g, '-')
      else if (dayCount >= 1) result = parseInt(dayCount) + ' ' + GLOBAL_CONFIG.date_suffix.day
      else if (hourCount >= 1) result = parseInt(hourCount) + ' ' + GLOBAL_CONFIG.date_suffix.hour
      else if (minuteCount >= 1) result = parseInt(minuteCount) + ' ' + GLOBAL_CONFIG.date_suffix.min
      else result = GLOBAL_CONFIG.date_suffix.just
    } else {
      result = parseInt(dateDiff / day)
    }
    return result
  },

  loadComment: (dom, callback) => {
    if ('IntersectionObserver' in window) {
      const observer = new IntersectionObserver((entries) => {
        if (entries[0].isIntersecting) {
          callback()
          observer.disconnect()
        }
      }, { threshold: [0] })
      observer.observe(dom)
    } else {
      callback()
    }
  },

  initJustifiedGallery: function (selector) {
    if (!(selector instanceof jQuery)) {
      selector = $(selector)
    }
    selector.each(function (i, o) {
      if ($(this).is(':visible')) {
        $(this).justifiedGallery({
          rowHeight: 220,
          margins: 4
        })
      }
    })
  },

  /**
   * 根据文字生成头像 DataURL
   */
  getLetterAvatar: (name, size = 100) => {
    const canvas = document.createElement('canvas')
    canvas.width = size
    canvas.height = size
    const context = canvas.getContext('2d')

    // 背景颜色列表
    const colors = [
      '#1abc9c', '#2ecc71', '#3498db', '#9b59b6', '#34495e',
      '#16a085', '#27ae60', '#2980b9', '#8e44ad', '#2c3e50',
      '#f1c40f', '#e67e22', '#e74c3c', '#95a5a6', '#f39c12',
      '#d35400', '#c0392b', '#bdc3c7', '#7f8c8d'
    ]

    // 根据姓名哈希选择颜色
    let hash = 0
    for (let i = 0; i < name.length; i++) {
      hash = name.charCodeAt(i) + ((hash << 5) - hash)
    }
    const color = colors[Math.abs(hash) % colors.length]

    // 绘制背景
    context.fillStyle = color
    context.fillRect(0, 0, size, size)

    // 绘制文字
    context.fillStyle = '#FFF'
    context.font = `${size * 0.6}px Arial`
    context.textAlign = 'center'
    context.textBaseline = 'middle'
    const letter = name.charAt(0).toUpperCase()
    context.fillText(letter, size / 2, size / 2)

    return canvas.toDataURL('image/png')
  },

  /**
   * 本地存储工具类 (统一自 init.js 和 utils.js)
   */
  optLocal: {
    set: (key, value, ttl) => {
      if (ttl === 0) return
      const now = new Date()
      const expiryDay = ttl * 86400000
      const item = {
        value: value,
        expiry: ttl ? now.getTime() + expiryDay : null
      }
      localStorage.setItem(key, JSON.stringify(item))
    },
    get: (key) => {
      const itemStr = localStorage.getItem(key)
      if (!itemStr) return undefined
      
      const item = JSON.parse(itemStr)
      const now = new Date()

      if (item.expiry && now.getTime() > item.expiry) {
        localStorage.removeItem(key)
        return undefined
      }
      return item.value
    }
  }
}
