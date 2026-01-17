package fun.flyingpig.note.util;

import java.util.ArrayList;
import java.util.List;

public class TextUtil {

    /**
     * 文本分块
     */
    public static List<String> splitIntoChunks(String text, int chunkSize) {
        List<String> chunks = new ArrayList<>();
        if (text == null || text.isEmpty()) {
            return chunks;
        }

        // 1. 清理文本
        String cleanedText = cleanMarkdown(text);

        // 2. 按段落分割并累积
        String[] paragraphs = cleanedText.split("\n\n+");
        StringBuilder currentChunk = new StringBuilder();

        for (String paragraph : paragraphs) {
            paragraph = paragraph.trim();
            if (paragraph.isEmpty()) continue;

            // 单段超长，需要切分
            if (paragraph.length() > chunkSize) {
                // 先保存当前累积的
                if (currentChunk.length() > 0) {
                    chunks.add(currentChunk.toString().trim());
                    currentChunk = new StringBuilder();
                }
                // 切分长段落
                chunks.addAll(splitLongParagraph(paragraph, chunkSize));
                continue;
            }

            // 加入后会超长，先保存
            if (currentChunk.length() + paragraph.length() > chunkSize && currentChunk.length() > 0) {
                chunks.add(currentChunk.toString().trim());
                currentChunk = new StringBuilder();
            }

            if (currentChunk.length() > 0) {
                currentChunk.append("\n\n");
            }
            currentChunk.append(paragraph);
        }

        if (currentChunk.length() > 0) {
            chunks.add(currentChunk.toString().trim());
        }

        // 3. 合并过短的块
        return mergeSmallChunks(chunks, chunkSize);
    }

    /**
     * 切分超长段落（按句子）
     */
    private static List<String> splitLongParagraph(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        // 按中英文句号切分
        String[] sentences = text.split("(?<=[。！？.!?])");
        StringBuilder buffer = new StringBuilder();
        for (String sentence : sentences) {
            if (buffer.length() + sentence.length() > chunkSize && buffer.length() > 0) {
                result.add(buffer.toString().trim());
                buffer = new StringBuilder();
            }
            buffer.append(sentence);
        }

        if (buffer.length() > 0) {
            result.add(buffer.toString().trim());
        }

        // 如果还有超长的（没有标点的情况），硬切
        List<String> finalResult = new ArrayList<>();
        for (String chunk : result) {
            if (chunk.length() > chunkSize) {
                for (int i = 0; i < chunk.length(); i += chunkSize) {
                    finalResult.add(chunk.substring(i, Math.min(i + chunkSize, chunk.length())));
                }
            } else {
                finalResult.add(chunk);
            }
        }

        return finalResult;
    }

    /**
     * 合并过短的相邻块
     */
    private static List<String> mergeSmallChunks(List<String> chunks, int chunkSize) {
        if (chunks.size() <= 1) return chunks;

        List<String> result = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();

        for (String chunk : chunks) {
            if (buffer.length() + chunk.length() + 2 <= chunkSize) {
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

    /**
     * 清理Markdown特殊元素
     */
    private static String cleanMarkdown(String text) {
        // 移除图片 ![alt](url)
        text = text.replaceAll("!\\[.*?]\\(.*?\\)", "");
        // 移除链接但保留文字 [text](url) -> text
        text = text.replaceAll("\\[([^]]*)]\\([^)]*\\)", "$1");
        // 移除HTML标签
        text = text.replaceAll("<[^>]+>", "");
        // 移除多余空行
        text = text.replaceAll("\n{3,}", "\n\n");
        return text.trim();
    }
}