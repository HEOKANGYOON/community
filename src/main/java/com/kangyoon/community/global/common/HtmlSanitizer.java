package com.kangyoon.community.global.common;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;
import org.springframework.stereotype.Component;

@Component
public class HtmlSanitizer {

    private static final PolicyFactory POLICY = new HtmlPolicyBuilder()
            .allowElements("p", "br", "strong", "em", "u", "s", "ul", "ol", "li",
                    "a", "img", "h1", "h2", "h3", "h4", "blockquote", "code", "pre",
                    "table", "thead", "tbody", "tr", "th", "td", "span", "div")
            .allowUrlProtocols("https")
            .allowAttributes("href").onElements("a")
            .allowAttributes("src", "alt", "width", "height").onElements("img")
            .allowAttributes("class").onElements("span", "div", "code", "pre") // Toast UI가 문법 강조 등에 class를 쓸 수 있어서
            .requireRelNofollowOnLinks()
            .toFactory();

    public String sanitize(String rawHtml) {
        if (rawHtml == null) return null;
        return POLICY.sanitize(rawHtml);
    }
}