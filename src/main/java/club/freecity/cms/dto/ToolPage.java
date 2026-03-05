package club.freecity.cms.dto;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import java.util.Collections;
import java.util.List;

@Data
public class ToolPage {
    private String path;
    private String lastmod;

    public static List<ToolPage> loadFromClasspath() {
        try {
            Resource res = new ClassPathResource("/static/tools-sitemap.json");
            if (!res.exists()) return Collections.emptyList();
            ObjectMapper mapper = new ObjectMapper();
            return mapper.readValue(res.getInputStream(), new TypeReference<List<ToolPage>>() {});
        } catch (Exception ignored) {
            return Collections.emptyList();
        }
    }
}

