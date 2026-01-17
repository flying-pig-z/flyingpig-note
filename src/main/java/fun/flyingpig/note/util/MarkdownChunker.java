package fun.flyingpig.note.util;

import java.util.*;
import java.util.regex.*;

public class MarkdownChunker {

    private static final int MAX_SIZE = 1000;

    /**
     * 对Markdown文本进行分块
     */
    public static List<String> split(String text) {
        if (text == null || text.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 0. 预处理：移除图片链接和无关HTML标签
        text = removeMarkdownImages(text);

        List<String> chunks = new ArrayList<>();

        // 1. 按标题切分
        String[] sections = text.split("(?=^#{1,6}\\s)", Pattern.MULTILINE);

        for (String section : sections) {
            section = section.trim();
            if (section.isEmpty()) continue;

            if (section.length() <= MAX_SIZE) {
                chunks.add(section);
            } else {
                chunks.addAll(splitByParagraph(section));
            }
        }

        // 2. 合并过短的块
        return mergeSmall(chunks);
    }

    /**
     * 移除Markdown图片链接和无关HTML标签
     * 支持格式：
     * - ![alt](url)
     * - ![alt](url "title")
     * - [![alt](img-url)](link-url) 带链接的图片
     * - <meta> 标签（如 referrer 配置）
     */
    private static String removeMarkdownImages(String text) {
        // 移除开头的 <meta> 标签及其后的换行（如 <meta name="referrer" content="no-referrer"/>）
        text = text.replaceAll("^\\s*<meta[^>]*/?>\\s*", "");
        // 移除带链接的图片 [![alt](img)](link)
        text = text.replaceAll("\\[!\\[.*?\\]\\(.*?\\)\\]\\(.*?\\)", "");
        // 移除普通图片 ![alt](url) 或 ![alt](url "title")
        text = text.replaceAll("!\\[.*?\\]\\(.*?\\)", "");
        // 移除HTML img标签
        text = text.replaceAll("<img[^>]*>", "");
        // 清理多余空行
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }

    /**
     * 按段落切分
     */
    private static List<String> splitByParagraph(String text) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String para : text.split("\n\n+")) {
            para = para.trim();
            if (para.isEmpty()) continue;

            if (buffer.length() + para.length() > MAX_SIZE && buffer.length() > 0) {
                result.add(buffer.toString());
                buffer = new StringBuilder();
            }

            // 单段超长，硬切
            if (para.length() > MAX_SIZE) {
                if (buffer.length() > 0) {
                    result.add(buffer.toString());
                    buffer = new StringBuilder();
                }
                result.addAll(hardSplit(para));
                continue;
            }

            if (buffer.length() > 0) buffer.append("\n\n");
            buffer.append(para);
        }

        if (buffer.length() > 0) {
            result.add(buffer.toString());
        }
        return result;
    }

    /**
     * 硬切分
     */
    private static List<String> hardSplit(String text) {
        List<String> result = new ArrayList<>();
        for (int i = 0; i < text.length(); i += MAX_SIZE) {
            result.add(text.substring(i, Math.min(i + MAX_SIZE, text.length())));
        }
        return result;
    }

    /**
     * 合并过短的相邻块
     */
    private static List<String> mergeSmall(List<String> chunks) {
        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String chunk : chunks) {
            if (buffer.length() + chunk.length() <= MAX_SIZE) {
                if (buffer.length() > 0) buffer.append("\n\n");
                buffer.append(chunk);
            } else {
                if (buffer.length() > 0) result.add(buffer.toString());
                buffer = new StringBuilder(chunk);
            }
        }

        if (buffer.length() > 0) result.add(buffer.toString());
        return result;
    }
}