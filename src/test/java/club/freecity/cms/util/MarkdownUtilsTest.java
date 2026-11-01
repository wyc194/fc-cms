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
    @DisplayName("Emoji 连续出现、句尾出现及纯 Emoji 输入均应原样保留")
    void testEmojiVariantsRoundTrip() {
        // 连续 emoji + 句尾 emoji（无尾随文本的边界情况）
        String text = "🐳🐋🐳 end 🐳";
        assertEquals(text, MarkdownUtils.sanitize(text), "连续/句尾 Emoji 应原样保留");

        // 纯 Emoji 输入经纯文本清洗（标题/摘要路径）同样完整还原
        String emojiOnly = "🐳";
        assertEquals(emojiOnly, MarkdownUtils.sanitizeText(emojiOnly), "纯 Emoji 经 sanitizeText 应原样保留");
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

    @Test
    @DisplayName("实体编码的攻击载荷不得因反转义而复活为真实标签（实体走私）")
    void testEntitySmugglingBlocked() {
        // 实体编码的 <img>，若全量反转义会复活为可执行标签（CSP unsafe-inline 不拦截内联事件）
        String smuggled = "&lt;img src=x onerror=alert(1)&gt;";
        String sanitized = MarkdownUtils.sanitize(smuggled);

        assertFalse(sanitized.contains("<img"), "反转义后不得出现真实标签");
        // 保持转义形态，浏览器渲染为纯文本
        assertTrue(sanitized.contains("&lt;img"), "标记字符应保持转义，按原文显示");

        // 十进制实体变体
        String smuggledDecimal = "&#60;img src=x onerror=alert(1)&#62;";
        assertFalse(MarkdownUtils.sanitize(smuggledDecimal).contains("<img"));
    }

    @Test
    @DisplayName("昵称中的引号保持转义，防止模板属性逃逸")
    void testQuoteKeptEscaped() {
        String nickname = "x\" onmouseover=\"alert(1)";
        String sanitized = MarkdownUtils.sanitizeText(nickname);

        // 原始 " 不得出现（避免 insertAdjacentHTML 模板中 ${nickname} 的属性逃逸）
        assertFalse(sanitized.contains("\""), "双引号应保持转义");
        // OWASP 将 " 序列化为 &#34;
        assertTrue(sanitized.contains("&#34;"));
    }

    @Test
    @DisplayName("普通文本中的等号等安全字符应被正常还原显示")
    void testSafeEntitiesDecoded() {
        String text = "a=b 与 c&#61;d 及 🐳";
        String sanitized = MarkdownUtils.sanitize(text);

        assertTrue(sanitized.contains("a=b"), "等号应还原显示");
        assertTrue(sanitized.contains("🐳"), "Emoji 应还原");
        assertFalse(sanitized.contains("&#61;"), "安全实体的转义不应存留");
    }

    @Test
    @DisplayName("许可标签保持原始形态，不被二次转义")
    void testAllowedTagsStayRaw() {
        String html = "<b>加粗</b> <a href=\"https://example.com\">链接</a>";
        String sanitized = MarkdownUtils.sanitize(html);

        assertTrue(sanitized.contains("<b>"), "许可标签应为原始形态");
        // LINKS 策略会给链接追加 rel="nofollow"，这里只断言 href 前缀
        assertTrue(sanitized.contains("<a href=\"https://example.com\""), "许可链接应保留");
    }
}
