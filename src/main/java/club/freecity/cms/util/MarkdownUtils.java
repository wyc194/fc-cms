package club.freecity.cms.util;

import com.vladsch.flexmark.html.HtmlRenderer;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.data.MutableDataSet;
import com.vladsch.flexmark.ext.tables.TablesExtension;
import com.vladsch.flexmark.ext.anchorlink.AnchorLinkExtension;
import com.vladsch.flexmark.ext.autolink.AutolinkExtension;
import com.vladsch.flexmark.ext.gfm.strikethrough.StrikethroughExtension;
import com.vladsch.flexmark.util.ast.Node;
import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.owasp.html.Sanitizers;
import org.unbescape.html.HtmlEscape;
import java.util.Arrays;

/**
 * Markdown 工具类，用于将 Markdown 转换为 HTML
 */
public class MarkdownUtils {

    private static final Parser PARSER;
    private static final HtmlRenderer RENDERER;
    private static final PolicyFactory POLICY;

    static {
        MutableDataSet options = new MutableDataSet();
        // 设置扩展：表格、自动链接、标题锚点、删除线
        options.set(Parser.EXTENSIONS, Arrays.asList(
                TablesExtension.create(),
                AutolinkExtension.create(),
                AnchorLinkExtension.create(),
                StrikethroughExtension.create()
        ));
        // 设置标题锚点（用于生成目录）
        options.set(AnchorLinkExtension.ANCHORLINKS_WRAP_TEXT, true);
        options.set(HtmlRenderer.GENERATE_HEADER_ID, true);
        options.set(HtmlRenderer.RENDER_HEADER_ID, true);
        
        PARSER = Parser.builder(options).build();
        RENDERER = HtmlRenderer.builder(options).build();

        // 初始化 HTML 清洗策略：允许基础文本格式、块、链接、图片、表格
        // 关键：必须显式允许 pre 和 code 标签及其 class 属性，否则会被 Sanitizers 过滤掉
        POLICY = Sanitizers.FORMATTING
                .and(Sanitizers.BLOCKS)
                .and(Sanitizers.LINKS)
                .and(Sanitizers.IMAGES)
                .and(Sanitizers.TABLES)
                .and(new HtmlPolicyBuilder()
                        .allowElements("pre", "code")
                        .allowAttributes("class").onElements("code")
                        .allowAttributes("id", "name").globally()
                        .allowAttributes("href", "target", "rel").onElements("a")
                        .toFactory());
    }

    /**
     * 将 Markdown 转换为 HTML 并进行安全清洗
     * @param markdown Markdown 内容
     * @return 安全的 HTML 内容
     */
    public static String renderHtml(String markdown) {
        if (markdown == null || markdown.trim().isEmpty()) {
            return "";
        }
        Node document = PARSER.parse(markdown);
        String unsafeHtml = RENDERER.render(document);
        return POLICY.sanitize(unsafeHtml);
    }

    /**
     * 对 HTML 内容进行安全清洗，并保留原始字符（支持 Emoji、中文标点等）
     * @param html HTML 内容
     * @return 安全的 HTML 内容
     */
    public static String sanitize(String html) {
        if (html == null || html.trim().isEmpty()) {
            return "";
        }
        // OWASP Sanitizer 会将 Emoji 等非 ASCII 字符转义为 HTML 实体
        // 使用 unbescape 进行反转义，它能正确处理代理对（Surrogate Pairs），防止 Emoji 乱码
        return HtmlEscape.unescapeHtml(POLICY.sanitize(html));
    }

    /**
     * 对纯文本内容进行安全清洗，移除所有 HTML 标签，并保留原始字符（支持 Emoji 等）
     * 适用于标题、摘要等不需要 HTML 标签的字段
     * @param text 原始文本
     * @return 清洗后的纯文本
     */
    public static String sanitizeText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "";
        }
        // 使用一个没有任何标签许可的策略来移除所有 HTML 标签
        String sanitized = new HtmlPolicyBuilder().toFactory().sanitize(text);
        return HtmlEscape.unescapeHtml(sanitized);
    }
}
