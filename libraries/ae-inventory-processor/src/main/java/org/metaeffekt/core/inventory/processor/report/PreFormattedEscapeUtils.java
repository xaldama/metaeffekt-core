package org.metaeffekt.core.inventory.processor.report;

import org.apache.commons.lang.StringEscapeUtils;

import java.util.HashMap;
import java.util.Map;

public class PreFormattedEscapeUtils {

    private Map<String, String> symbols = new HashMap<>();
    private Map<String, String> characters = new HashMap<>();
    private Map<String, String> tags = new HashMap<>();

    public PreFormattedEscapeUtils() {
        putEscaped(symbols, "&copy;");

        putEscaped(characters, "&lt;");
        putEscaped(characters, "&gt;");

        putEscaped(tags, "<lq>");
        putEscaped(tags, "</lq>");

        putEscaped(tags, "<p>");
        putEscaped(tags, "</p>");
        putEscaped(tags, "<p/>");

        putEscaped(tags, "<ol>");
        putEscaped(tags, "</ol>");

        putEscaped(tags, "<ul>");
        putEscaped(tags, "</ul>");

        putEscaped(tags, "<li>");
        putEscaped(tags, "</li>");

        putEscaped(tags, "<codeph>");
        putEscaped(tags, "</codeph>");

        putEscaped(tags, "<i>");
        putEscaped(tags, "</i>");

        putEscaped(tags, "<b>");
        putEscaped(tags, "</b>");

        putEscapedAndRedefine(tags, "<h1>", "<p><b>");
        putEscapedAndRedefine(tags, "</h1>", "</b></p>");

        putEscapedAndRedefine(tags, "<h2>", "<p><b>");
        putEscapedAndRedefine(tags, "</h2>", "</b></p>");

        putEscapedAndRedefine(tags, "<h3>", "<p><b>");
        putEscapedAndRedefine(tags, "</h3>", "</b></p>");

    }

    private void putEscaped(Map<String, String> map, String value) {
        map.put(value, StringEscapeUtils.escapeXml(value));
    }

    private void putEscapedAndRedefine(Map<String, String> map, String value, String refinedValue) {
        map.put(value, StringEscapeUtils.escapeXml(value));
    }

    public String xml(String string) {
        if (string == null)  return null;
        String escaped = string;

        // pass 1: start and end tags
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        // pass 2: symbols
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        // pass 3: chars
        for (Map.Entry<String, String> entry : characters.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        escaped = StringEscapeUtils.escapeXml(StringEscapeUtils.unescapeXml(escaped));

        // pass 1: start and end tags
        for (Map.Entry<String, String> entry : tags.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        // pass 2: symbols
        for (Map.Entry<String, String> entry : symbols.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        // pass 3: chars
        for (Map.Entry<String, String> entry : characters.entrySet()) {
            escaped = escaped.replace(entry.getValue(), entry.getKey());
        }

        return escaped;
    }

    public Object purl(String string) {
        return string.replace("@", "%40");
    }
}
