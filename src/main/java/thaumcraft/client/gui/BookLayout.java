package thaumcraft.client.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Вёрстка текста таумономикона. Разбирает разметку оригинала (&lt;BR&gt;, &lt;LINE&gt;, &lt;IMG&gt;, заголовки вида
 * "§l   §nНазвание§r") и раскладывает её по страницам так, чтобы ничего не вылезало за край: что не поместилось,
 * переходит на следующую страницу.
 */
public final class BookLayout {
    /**
     * Обычный шрифт Minecraft: гладкий книжный шрифт пробовали, и он оказался хуже.
     */
    public static final Style BODY = Style.EMPTY;
    private static final Pattern HEADING = Pattern.compile("^§l\\s*§n(.+?)§r\\s*$");
    private static final Pattern IMAGE = Pattern.compile("<IMG>([^:<]+:[^:<]+):(\\d+):(\\d+):(\\d+):(\\d+):([\\d.]+)</IMG>");
    private static final Pattern TOKEN = Pattern.compile("<IMG>.*?</IMG>|<LINE/?>|<BR>");
    private static final int LINE_HEIGHT = 10;
    private static final int PARAGRAPH_GAP = 3;

    /** Один элемент страницы со смещением от её верха. */
    public sealed interface Element permits TextLine, Rule, Image {
        int y();
    }

    /** Строка текста; centered — заголовок по центру страницы. */
    public record TextLine(int y, FormattedCharSequence text, boolean centered) implements Element {
    }

    /** Горизонтальная линейка-разделитель. */
    public record Rule(int y) implements Element {
    }

    /** Картинка: участок текстуры в координатах листа 256x256, нарисованный с масштабом. */
    public record Image(int y, ResourceLocation texture, int u, int v, int width, int height, float scale) implements Element {
        public int drawnHeight() {
            return Math.round(height * scale);
        }

        public int drawnWidth() {
            return Math.round(width * scale);
        }
    }

    /**
     * @param text        текст страницы с разметкой оригинала
     * @param width       ширина страницы
     * @param firstHeight высота первой страницы (на ней место занято заголовком исследования)
     * @param height      высота остальных страниц
     */
    public static List<List<Element>> layout(Font font, String text, int width, int firstHeight, int height) {
        List<List<Element>> pages = new ArrayList<>();
        List<Element> current = new ArrayList<>();
        pages.add(current);
        int y = 0;
        int limit = firstHeight;

        List<String> tokens = tokenize(text);
        for (String token : tokens) {
            int needed;
            // заголовки, картинки и линейки не разрываются между страницами; обычный текст перетекает построчно
            boolean keepTogether = true;
            List<Element> block = new ArrayList<>();
            if (token.equals("<BR>")) {
                continue;
            } else if (token.startsWith("<LINE")) {
                block.add(new Rule(4));
                needed = 12;
            } else if (token.startsWith("<IMG>")) {
                Matcher m = IMAGE.matcher(token);
                if (!m.matches()) {
                    continue;
                }
                Image image = new Image(2, texture(m.group(1)), Integer.parseInt(m.group(2)), Integer.parseInt(m.group(3)),
                        Integer.parseInt(m.group(4)), Integer.parseInt(m.group(5)), Float.parseFloat(m.group(6)));
                block.add(image);
                needed = image.drawnHeight() + 4;
            } else {
                Matcher heading = HEADING.matcher(token);
                boolean isHeading = heading.matches();
                // у заголовка пробелы для центровки отбрасываются: он ставится по центру сам
                String content = isHeading ? "§l§n" + heading.group(1).trim() + "§r" : token;
                int lineY = 0;
                for (FormattedCharSequence line : font.split(styled(content), width)) {
                    block.add(new TextLine(lineY, line, isHeading));
                    lineY += LINE_HEIGHT;
                }
                needed = lineY + PARAGRAPH_GAP;
                keepTogether = isHeading;
                if (isHeading) {
                    needed += 2;
                }
            }

            if (keepTogether && y + needed - PARAGRAPH_GAP > limit && y > 0 && needed <= height) {
                current = new ArrayList<>();
                pages.add(current);
                y = 0;
                limit = height;
            }
            for (Element element : block) {
                int elementHeight = element instanceof Image image ? image.drawnHeight() : LINE_HEIGHT;
                if (y + element.y() + elementHeight > limit && element instanceof TextLine line) {
                    int shift = line.y();
                    current = new ArrayList<>();
                    pages.add(current);
                    limit = height;
                    y = -shift;
                }
                current.add(move(element, y + element.y()));
            }
            y += needed;
        }
        return pages;
    }

    /**
     * Разбирает коды оформления "§x" в настоящие стили. Если оставить их в строке, игра при переносе строки
     * сбрасывает стиль к обычному и продолжение абзаца рисуется другим шрифтом.
     */
    private static Component styled(String text) {
        MutableComponent out = Component.empty();
        Style style = BODY;
        StringBuilder run = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '§' && i + 1 < text.length()) {
                ChatFormatting format = ChatFormatting.getByCode(Character.toLowerCase(text.charAt(i + 1)));
                i++;
                if (format == null) {
                    continue;
                }
                if (run.length() > 0) {
                    out.append(Component.literal(run.toString()).withStyle(style));
                    run.setLength(0);
                }
                if (format == ChatFormatting.RESET) {
                    style = BODY;
                } else if (format.isColor()) {
                    // как в ванильных кодах: цвет сбрасывает жирность, подчёркивание и прочее
                    style = BODY.withColor(format);
                } else {
                    style = style.applyFormat(format);
                }
            } else {
                run.append(c);
            }
        }
        if (run.length() > 0) {
            out.append(Component.literal(run.toString()).withStyle(style));
        }
        return out;
    }

    private static List<String> tokenize(String text) {
        List<String> tokens = new ArrayList<>();
        Matcher m = TOKEN.matcher(text);
        int last = 0;
        while (m.find()) {
            add(tokens, text.substring(last, m.start()));
            tokens.add(m.group());
            last = m.end();
        }
        add(tokens, text.substring(last));
        return tokens;
    }

    private static void add(List<String> tokens, String chunk) {
        if (!chunk.isBlank()) {
            tokens.add(chunk.strip());
        }
    }

    /** Пути оригинала ведут в старые папки и бывают с заглавными буквами. */
    private static ResourceLocation texture(String path) {
        String fixed = path.toLowerCase().replace("textures/items/", "textures/item/").replace("textures/blocks/", "textures/block/");
        return thaumcraft.common.Res.parse(fixed);
    }

    private static Element move(Element element, int y) {
        if (element instanceof TextLine line) {
            return new TextLine(y, line.text(), line.centered());
        }
        if (element instanceof Image image) {
            return new Image(y, image.texture(), image.u(), image.v(), image.width(), image.height(), image.scale());
        }
        return new Rule(y);
    }

    private BookLayout() {
    }
}
