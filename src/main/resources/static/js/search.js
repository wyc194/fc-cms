/**
 * search.js - 即时搜索逻辑 (支持无限滚动)
 */
document.addEventListener('DOMContentLoaded', () => {
    const $searchMask = document.getElementById('search-mask')
    const $localSearch = document.getElementById('local-search')
    const $input = document.querySelector('#local-search .local-search-box--input')
    const $resultContent = document.getElementById('local-search-results')
    const $resultList = $resultContent.querySelector('.search-result-list')
    const $resultStats = $resultContent.querySelector('.search-result-stats')
    const $searchButtons = document.querySelectorAll('#search-button .search')
    const $closeButton = document.querySelector('.search-close-button')

    if (!$localSearch) return

    // 搜索状态管理
    let searchState = {
        keyword: '',
        page: 0,
        size: 5, // 增加每页条数以获得更好的滚动体验
        totalPages: 0,
        isLoading: false,
        hasMore: true
    }

    // 打开搜索框
    const openSearch = () => {
        $localSearch.style.display = 'block'
        $input.focus()
        document.body.style.overflow = 'hidden'
        document.addEventListener('keydown', onKeyDown)
    }

    // 关闭搜索框
    const closeSearch = () => {
        $localSearch.style.display = 'none'
        document.body.style.overflow = ''
        document.removeEventListener('keydown', onKeyDown)
    }

    // 按键处理
    const onKeyDown = (e) => {
        if (e.key === 'Escape') {
            closeSearch()
        }
    }

    // 绑定基础事件
    $searchButtons.forEach(btn => btn.addEventListener('click', openSearch))
    $searchMask.addEventListener('click', closeSearch)
    $closeButton.addEventListener('click', closeSearch)

    /**
     * 执行搜索请求
     * @param {boolean} isNewSearch 是否为新搜索（重置状态）
     */
    const performSearch = async (isNewSearch = false) => {
        if (searchState.isLoading) return
        
        const keyword = $input.value.trim()
        if (!keyword) {
            resetSearch()
            return
        }

        if (isNewSearch) {
            searchState = {
                keyword,
                page: 0,
                size: 10,
                totalPages: 0,
                isLoading: true,
                hasMore: true
            }
            $resultList.innerHTML = ''
            $resultStats.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 正在搜索...'
        } else {
            if (!searchState.hasMore) return
            searchState.isLoading = true
            showLoadingIndicator()
        }

        try {
            const response = await fetch(`/api/search/instant?keyword=${encodeURIComponent(keyword)}&page=${searchState.page}&size=${searchState.size}`)
            if (!response.ok) throw new Error('Search failed')
            
            const data = await response.json()
            const items = data.content
            
            searchState.totalPages = data.totalPages
            searchState.hasMore = searchState.page < data.totalPages - 1
            
            renderResults(items, isNewSearch, data.totalElements)
            
            searchState.page++
        } catch (error) {
            console.error('Search error:', error)
            $resultStats.innerHTML = '搜索服务暂时不可用'
        } finally {
            searchState.isLoading = false
            hideLoadingIndicator()
        }
    }

    /**
     * 渲染结果
     */
    const renderResults = (items, isNewSearch, totalElements) => {
        if (isNewSearch) {
            if (items.length === 0) {
                $resultStats.innerHTML = `找不到关于 "<b>${searchState.keyword}</b>" 的文章`
                return
            }
            $resultStats.innerHTML = `找到 ${totalElements} 篇文章`
        }

        const fragment = document.createDocumentFragment()
        items.forEach(item => {
            const li = document.createElement('li')
            li.className = 'search-result-item'
            
            // 高亮关键词逻辑
            const highlight = (text) => {
                if (!text) return ''
                return text.replace(new RegExp(searchState.keyword, 'gi'), match => `<span class="search-keyword">${match}</span>`)
            }

            li.innerHTML = `
                <a href="/article/${item.id}">
                    <div class="search-result-title">${highlight(item.title)}</div>
                    <div class="search-result-content">${highlight(item.summary)}</div>
                </a>
            `
            fragment.appendChild(li)
        })
        $resultList.appendChild(fragment)

        if (!searchState.hasMore && totalElements > 0) {
            showNoMoreIndicator()
        }
    }

    /**
     * 重置搜索
     */
    const resetSearch = () => {
        searchState = {
            keyword: '',
            page: 0,
            size: 10,
            totalPages: 0,
            isLoading: false,
            hasMore: true
        }
        $resultList.innerHTML = ''
        $resultStats.innerHTML = '请在上方输入关键词进行搜索'
        hideLoadingIndicator()
        hideNoMoreIndicator()
    }

    // 加载/无更多数据的指示器
    const showLoadingIndicator = () => {
        if (!document.querySelector('.search-loading')) {
            const div = document.createElement('div')
            div.className = 'search-loading'
            div.innerHTML = '<i class="fas fa-spinner fa-spin"></i> 正在加载更多...'
            $resultContent.appendChild(div)
        }
    }

    const hideLoadingIndicator = () => {
        const el = document.querySelector('.search-loading')
        if (el) el.remove()
    }

    const showNoMoreIndicator = () => {
        if (!document.querySelector('.search-no-more')) {
            const div = document.createElement('div')
            div.className = 'search-no-more'
            div.innerHTML = '没有更多结果了'
            $resultContent.appendChild(div)
        }
    }

    const hideNoMoreIndicator = () => {
        const el = document.querySelector('.search-no-more')
        if (el) el.remove()
    }

    // 防抖处理输入搜索
    const debounce = (func, wait) => {
        let timeout
        return function (...args) {
            clearTimeout(timeout)
            timeout = setTimeout(() => func.apply(this, args), wait)
        }
    }

    $input.addEventListener('input', debounce(() => performSearch(true), 300))

    // 无限滚动逻辑
    $resultContent.addEventListener('scroll', () => {
        if (searchState.isLoading || !searchState.hasMore) return

        const scrollHeight = $resultContent.scrollHeight
        const scrollTop = $resultContent.scrollTop
        const clientHeight = $resultContent.clientHeight

        // 距离底部 50px 时触发加载
        if (scrollTop + clientHeight >= scrollHeight - 50) {
            performSearch(false)
        }
    })
})
