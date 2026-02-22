package club.freecity.cms.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MarkdownUtilsTest {

    @Test
    @DisplayName("验证 Emoji 字符在清洗后是否能原样保留")
    void testEmojiSanitization() {
        // 🐳 是 Supplementary Character (U+1F433)，占用两个 char
        String emojiText = "关于我 🐳 这里的环境真好！";
        
        // 执行清洗
        String sanitized = MarkdownUtils.sanitize(emojiText);
        
        // 验证：不应出现乱码（如小方块），也不应是转义后的实体（如 &#128051;）
        assertEquals(emojiText, sanitized, "Emoji 字符应该被原样还原，不应出现截断或转义存留");
    }

    @Test
    @DisplayName("验证 HTML 标签清洗功能")
    void testHtmlSanitization() {
        String unsafeHtml = "<script>alert('xss')</script><p>Hello 🐳</p>";
        String expected = "<p>Hello 🐳</p>";
        
        String sanitized = MarkdownUtils.sanitize(unsafeHtml);
        
        assertEquals(expected, sanitized, "应移除危险标签并保留安全内容及 Emoji");
    }

    @Test
    @DisplayName("验证纯文本清洗功能")
    void testSanitizeText() {
        String mixedText = "<h1>标题</h1> 🐳 内容";
        
        String sanitized = MarkdownUtils.sanitizeText(mixedText);
        
        // 移除所有标签，但保留 Emoji
        assertTrue(sanitized.contains("🐳"));
        assertFalse(sanitized.contains("<h1>"));
    }
}
