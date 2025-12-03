package com.oddlabs.tt.font;

import org.jspecify.annotations.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class TextLayout {

    public record Line(CharSequence content, int startIndex) {}

    private final @NonNull Font font;
    private final @NonNull CharSequence text;
    private final int wrapWidth;
    private final @NonNull List<@NonNull Line> lines;
    private final int totalHeight;

    public TextLayout(@NonNull Font font, @NonNull CharSequence text, int wrapWidth) {
        this.font = font;
        this.text = text;
        this.wrapWidth = wrapWidth;
        this.lines = calculateWordWrap();
        this.totalHeight = lines.size() * font.getHeight();
    }

    public @NonNull Font getFont() {
        return font;
    }

    public @NonNull List<@NonNull Line> getLines() {
        return lines;
    }

    public int getTextHeight() {
        return totalHeight;
    }

    public int getCursorLine(int index) {
        if (index < 0 || index > text.length()) {
            return 0;
        }
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            int lineEnd = line.startIndex() + line.content().length();
            if (index >= line.startIndex() && index <= lineEnd) {
                return i;
            }
        }
        return lines.size() - 1;
    }

    public int getCursorX(int index) {
        if (index < 0 || index > text.length()) {
            return 0;
        }

        for (Line line : lines) {
            int lineEnd = line.startIndex() + line.content().length();
            if (index >= line.startIndex() && index <= lineEnd) {
                int indexInLine = index - line.startIndex();
                return font.getWidth(line.content().subSequence(0, indexInLine));
            }
        }
        // Should not be reached if index is valid
        return 0;
    }

    public int getLineIndexAtY(float y, float textBlockHeight) {
        if (lines.isEmpty()) return 0;
        int lineHeight = font.getHeight();
        // Y is usually from top, so invert for line index from top
        int lineIndex = (int) ((textBlockHeight - y) / lineHeight);
        return Math.clamp(lineIndex, 0, lines.size() - 1);
    }

    public int getCharacterIndexAt(float x, float y, float textBlockHeight) {
        if (lines.isEmpty()) return 0;

        int lineIndex = getLineIndexAtY(y, textBlockHeight);
        Line targetLine = lines.get(lineIndex);

        int charIndexInLine = 0;
        float currentWidth = 0;
        for (int i = 0; i < targetLine.content().length(); i++) {
            char c = targetLine.content().charAt(i);
            float charWidth = font.getWidth(String.valueOf(c));
            if (x >= currentWidth && x < currentWidth + charWidth) {
                charIndexInLine = i;
                break;
            }
            currentWidth += charWidth;
            charIndexInLine = i + 1; // If click is past last char, set to end of line
        }
        return targetLine.startIndex() + charIndexInLine;
    }

    public int getLineStartCharIndex(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return 0;
        }
        return lines.get(lineIndex).startIndex();
    }

    public int getLineEndCharIndex(int lineIndex) {
        if (lineIndex < 0 || lineIndex >= lines.size()) {
            return text.length();
        }
        Line line = lines.get(lineIndex);
        return line.startIndex() + line.content().length();
    }

    public int getLineNumberForCharIndex(int charIndex) {
        if (charIndex < 0 || charIndex > text.length()) {
            return 0;
        }
        for (int i = 0; i < lines.size(); i++) {
            Line line = lines.get(i);
            if (charIndex >= line.startIndex() && charIndex <= line.startIndex() + line.content().length()) {
                return i;
            }
        }
        return 0; // Should not happen for valid charIndex
    }

    private @NonNull List<@NonNull Line> calculateWordWrap() {
        if (text.isEmpty()) {
            return Collections.emptyList();
        }

        List<Line> calculatedLines = new ArrayList<>();
        int lineStart = 0;

        while (lineStart < text.length()) {
            int lineEnd = findLineEnd(lineStart);
            calculatedLines.add(new Line(text.subSequence(lineStart, lineEnd).toString(), lineStart));
            lineStart = lineEnd;

            // Skip the newline or space that caused the break
            if (lineStart < text.length() && (text.charAt(lineStart) == '\n' || text.charAt(lineStart) == ' ')) {
                lineStart++;
            }
        }

        return Collections.unmodifiableList(calculatedLines);
    }

    private int findLineEnd(int lineStart) {
        int i = lineStart;
        int lastSpace = -1;

        while (i < text.length()) {
            char c = text.charAt(i);
            if (c == '\n') {
                return i; // Forced line break
            }

            if (Character.isWhitespace(c)) {
                lastSpace = i;
            }

            int currentWidth = font.getWidth(text.subSequence(lineStart, i + 1));
            if (currentWidth > wrapWidth) {
                // Word is longer than the line, break mid-word
                return lastSpace != -1 ? lastSpace : i; // Break at the last known space
            }
            i++;
        }
        return text.length(); // End of text
    }
}
