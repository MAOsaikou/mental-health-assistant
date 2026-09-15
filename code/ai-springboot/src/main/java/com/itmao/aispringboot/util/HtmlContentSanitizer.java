package com.itmao.aispringboot.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.safety.Safelist;

/**
 * 对需要以富文本形式展示的文章内容做服务端白名单清洗。
 */
public final class HtmlContentSanitizer {

    private static final Safelist ARTICLE_SAFELIST = new Safelist()
            .addTags(
                    "p", "br", "strong", "b", "em", "i", "u", "s",
                    "h1", "h2", "h3", "h4", "blockquote",
                    "ul", "ol", "li", "pre", "code", "a", "span", "div")
            .addAttributes("a", "href", "title")
            .addProtocols("a", "href", "http", "https", "mailto")
            .addEnforcedAttribute("a", "rel", "nofollow noopener noreferrer");

    private static final Document.OutputSettings OUTPUT_SETTINGS = new Document.OutputSettings()
            .prettyPrint(false);

    private HtmlContentSanitizer() {
    }

    public static String sanitize(String html) {
        if (html == null || html.isBlank()) {
            return html;
        }
        return Jsoup.clean(html, "", ARTICLE_SAFELIST, OUTPUT_SETTINGS);
    }
}
