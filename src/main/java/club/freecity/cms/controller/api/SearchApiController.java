package club.freecity.cms.controller.api;

import club.freecity.cms.annotation.RateLimit;
import club.freecity.cms.dto.ArticleDto;
import club.freecity.cms.service.ArticleService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 搜索相关的 API 接口
 */
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchApiController {

    private final ArticleService articleService;

    /**
     * 即时搜索 API
     * 只返回必要的字段（id, title, summary, thumbnail, createTime），用于前端展示
     */
    @RateLimit(window = 1, count = 5)
    @GetMapping("/instant")
    public ResponseEntity<Map<String, Object>> instantSearch(
            @RequestParam("keyword") String keyword,
            @PageableDefault(size = 5, sort = "createTime", direction = Sort.Direction.DESC) Pageable pageable) {
        
        // 使用现有的 searchArticles 方法，该方法目前只匹配 title 和 summary
        Page<ArticleDto> results = articleService.searchArticles(keyword, pageable);
        
        Map<String, Object> response = new HashMap<>();
        response.put("content", results.getContent());
        response.put("totalElements", results.getTotalElements());
        response.put("totalPages", results.getTotalPages());
        
        return ResponseEntity.ok(response);
    }
}
