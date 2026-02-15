package club.freecity.cms.controller.view;
import club.freecity.cms.common.ResultCode;
import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.exception.BusinessException;
import club.freecity.cms.service.ArticleService;
import club.freecity.cms.service.CategoryService;
import club.freecity.cms.service.TagService;
import club.freecity.cms.service.TenantService;
import club.freecity.cms.dto.TenantDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 读者端视图控制器 (Thymeleaf)
 */
@Slf4j
@Controller
@RequiredArgsConstructor
public class IndexController {

    private final ArticleService articleService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final TenantService tenantService;

    @GetMapping("/")
    public String index(Model model, 
                        @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("articles", articleService.listPublishedArticles(pageable));
        addAsideModelAttributes(model);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam("keyword") String keyword, Model model,
                         @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        model.addAttribute("articles", articleService.searchArticles(keyword, pageable));
        model.addAttribute("keyword", keyword);
        addAsideModelAttributes(model);
        return "index";
    }

    @GetMapping("/article/{id}")
    public String articleDetail(@PathVariable Long id, Model model,
                                @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        ArticleDto article = articleService.getArticleById(id);
        if (article == null || !Boolean.TRUE.equals(article.getPublished())) {
            throw new BusinessException(ResultCode.NOT_FOUND.getCode(), "文章不存在或未发布");
        }

        // 增加阅读量
        articleService.incrementViewCount(id);
        // 更新当前对象的阅读量，以便前端显示
        article.setViewCount(article.getViewCount() + 1);

        model.addAttribute("article", article);
        model.addAttribute("prevArticle", articleService.getPreviousArticle(article.getCreateTime()));
        model.addAttribute("nextArticle", articleService.getNextArticle(article.getCreateTime()));
        
        Long categoryId = article.getCategory() != null ? article.getCategory().getId() : null;
        model.addAttribute("relatedArticles", articleService.listRelatedArticles(article.getId(), categoryId));
        
        addAsideModelAttributes(model);
        return "article";
    }

    @GetMapping({"/categories", "/categories.html", "/category/{id}"})
    public String categories(@PathVariable(required = false) Long id, Model model,
                             @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        if (id != null) {
            model.addAttribute("articles", articleService.listPublishedArticlesByCategory(id, pageable));
            model.addAttribute("category", categoryService.getCategoryById(id));
            addAsideModelAttributes(model);
            return "index";
        }
        model.addAttribute("allCategories", categoryService.listCategoryTree());
        addAsideModelAttributes(model);
        return "categories";
    }

    @GetMapping({"/tags", "/tags.html", "/tag/{id}"})
    public String tags(@PathVariable(required = false) Long id, Model model,
                       @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        if (id != null) {
            model.addAttribute("articles", articleService.listPublishedArticlesByTag(id, pageable));
            model.addAttribute("tag", tagService.getTagById(id));
            addAsideModelAttributes(model);
            return "index";
        }
        model.addAttribute("allTags", tagService.listAllTags());
        addAsideModelAttributes(model);
        return "tags";
    }

    @GetMapping({"/archives", "/archives.html", "/archives/{year}"})
    public String archives(@PathVariable(required = false) String year,
                           Model model,
                           @PageableDefault(size = 10, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<ArticleDto> articlesPage;
        if (StringUtils.hasText(year)) {
            articlesPage = articleService.listPublishedArticlesByYear(year, pageable);
            model.addAttribute("currentYear", year);
        } else {
            articlesPage = articleService.listPublishedArticles(pageable);
        }
        return processArchives(model, articlesPage);
    }

    private String processArchives(Model model, Page<ArticleDto> articlesPage) {
        // 按年份分组
        Map<String, List<ArticleDto>> archiveArticles = articlesPage.getContent().stream()
                .collect(Collectors.groupingBy(
                        article -> String.valueOf(article.getCreateTime().getYear()),
                        TreeMap::new,
                        Collectors.toList()
                ));
        
        // 倒序排列年份
        Map<String, List<ArticleDto>> sortedArchives = new LinkedHashMap<>();
        archiveArticles.entrySet().stream()
                .sorted(Map.Entry.<String, List<ArticleDto>>comparingByKey().reversed())
                .forEachOrdered(x -> sortedArchives.put(x.getKey(), x.getValue()));

        model.addAttribute("archiveArticles", sortedArchives);
        model.addAttribute("currentPage", articlesPage.getNumber() + 1);
        model.addAttribute("totalPages", articlesPage.getTotalPages());
        
        addAsideModelAttributes(model);
        return "archives";
    }

    @GetMapping({"/about", "/about.html"})
    public String about(Model model) {
        addAsideModelAttributes(model);
        return "about";
    }

    @GetMapping({"/links", "/links.html"})
    public String links(Model model) {
        addAsideModelAttributes(model);
        TenantDto tenantConfig = (TenantDto) model.getAttribute("tenantConfig");
        if (tenantConfig != null && tenantConfig.getLinks() != null) {
            model.addAttribute("linksMap", tenantConfig.getLinks());
        }
        return "links";
    }

    @GetMapping({"/sitemap", "/sitemap.html"})
    public String sitemap(Model model) {
        model.addAttribute("allTags", tagService.listAllTags());
        model.addAttribute("allCategories", categoryService.listCategoryTree());
        // 获取所有已发布的文章（不分页，用于站点地图展示）
        model.addAttribute("allArticles", articleService.listPublishedArticles(Pageable.unpaged()).getContent());
        return "sitemap";
    }

    @GetMapping(value = "/sitemap.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public String sitemapXml(Model model, HttpServletRequest request) {
        // 获取基础 URL
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        
        model.addAttribute("baseUrl", baseUrl);
        model.addAttribute("articles", articleService.listPublishedArticles(Pageable.unpaged()).getContent());
        model.addAttribute("categories", categoryService.listCategoryTree());
        model.addAttribute("tags", tagService.listAllTags());
        
        return "sitemap_xml";
    }

    /**
     * 添加侧边栏通用模型属性
     */
    private void addAsideModelAttributes(Model model) {
        model.addAttribute("tenantConfig", tenantService.getCurrentTenantConfig());
        model.addAttribute("articleCount", articleService.countPublishedArticles());
        model.addAttribute("categoryCount", categoryService.countCategories());
        model.addAttribute("tagCount", tagService.countTags());
        
        // 侧边栏小组件数据
        model.addAttribute("recentArticles", articleService.listPublishedArticles(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createTime"))).getContent());
        model.addAttribute("asideCategories", categoryService.listCategoryTree());
        model.addAttribute("archives", articleService.listArchives());
        model.addAttribute("lastUpdateTime", articleService.getLastUpdateTime());
    }
}
