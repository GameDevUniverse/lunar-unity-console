package spacemadness.com.lunarconsole.console;

import android.graphics.Typeface;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.CharacterStyle;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import spacemadness.com.lunarconsole.utils.IntReference;

/* This class is not thread-safe */
public class DefaultRichTextFactory implements RichTextFactory {
    private final StyleSpan bold = new StyleSpan(Typeface.BOLD);
    private final StyleSpan italic = new StyleSpan(Typeface.ITALIC);
    private final StyleSpan boldItalic = new StyleSpan(Typeface.BOLD_ITALIC);
    private final Map<String, CharacterStyle> colorStyleMap;

    private final ColorFactory colorFactory;

    public DefaultRichTextFactory(ColorFactory colorFactory) {
        if (colorFactory == null)
        {
            throw new IllegalArgumentException("Color factory is null");
        }
        this.colorFactory = colorFactory;
        colorStyleMap = new HashMap<>();
    }

    @Override
    public CharSequence createRichText(String text) {
        List<LURichTextTag> tags = null;
        Stack<LURichTextTagInfo> stack = null;
        IntReference i = new IntReference(0);

        StringBuilder buffer = new StringBuilder(text.length());

        int boldCount = 0;
        int italicCount = 0;

        while (i.value < text.length())
        {
            char chr = text.charAt(i.value++);
            if (chr == '<')
            {
                LURichTextTagInfo tag = _tryCaptureTag(text, buffer.length(), i);
                if (tag != null)
                {
                    if (tag.open)
                    {
                        if ("b".equals(tag.name))
                        {
                            boldCount++;
                        }
                        else if ("i".equals(tag.name))
                        {
                            italicCount++;
                        }

                        if (stack == null) stack = new Stack<>();
                        stack.add(tag);
                    }
                    else if (stack != null && stack.size() > 0)
                    {
                        LURichTextTagInfo opposingTag = stack.pop();

                        // if tags don't match - just use raw string
                        if (!tag.name.equals(opposingTag.name))
                        {
                            continue;
                        }

                        if ("b".equals(tag.name))
                        {
                            boldCount--;
                            if (boldCount > 0)
                            {
                                continue;
                            }
                        }
                        else if ("i".equals(tag.name))
                        {
                            italicCount--;
                            if (italicCount > 0)
                            {
                                continue;
                            }
                        }

                        // create rich text tag
                        int len = buffer.length() - opposingTag.position;
                        if (len > 0)
                        {
                            if (tags == null) tags = new ArrayList<>();
                            switch (tag.name) {
                                case "b": {
                                    StyleSpan style = italicCount > 0 ? boldItalic : bold;
                                    tags.add(new LURichTextTag(style, opposingTag.position, len));
                                    break;
                                }
                                case "i": {
                                    StyleSpan style = boldCount > 0 ? boldItalic : italic;
                                    tags.add(new LURichTextTag(style, opposingTag.position, len));
                                    break;
                                }
                                case "color":
                                    String colorValue = opposingTag.attribute;
                                    if (colorValue != null) {
                                        CharacterStyle style = styleFromValue(colorValue);
                                        tags.add(new LURichTextTag(style, opposingTag.position, len));
                                    }
                                    break;
                            }
                        }
                    }
                }
                else
                {
                    buffer.append(chr);
                }
            }
            else
            {
                buffer.append(chr);
            }
        }

        if (tags != null && buffer.length() > 0)
        {
            return createSpannedString(buffer.toString(), tags);
        }

        if (buffer.length() < text.length())
        {
            return buffer.toString();
        }

        return text;
    }

    private SpannableString createSpannedString(String text, List<LURichTextTag> tags) {
        SpannableString string = new SpannableString(text);
        for (int i = 0; i < tags.size(); i++) {
            LURichTextTag tag = tags.get(i);
            string.setSpan(tag.style, tag.startIndex, tag.startIndex + tag.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return string;
    }

    //region Rich Text

    private static boolean _isvalidTagName(String name)
    {
        return name.equals("b") || name.equals("i") || name.equals("color");
    }

    private static LURichTextTagInfo _tryCaptureTag(String str, int position, IntReference iterPtr)
    {
        int end = iterPtr.value;
        boolean isOpen = true;
        if (end < str.length() && str.charAt(end) == '/')
        {
            isOpen = false;
            ++end;
        }

        int start = end;
        boolean found = false;
        while (end < str.length())
        {
            char chr = str.charAt(end++);
            if (chr == '>')
            {
                found = true;
                break;
            }
        }

        if (!found)
        {
            return null;
        }

        String capture = str.substring(start, end - 1);
        int index = capture.lastIndexOf('=');
        String name = index != -1 ? capture.substring(0, index) : capture;
        if (!_isvalidTagName(name))
        {
            return null;
        }

        String attribute = index != -1 ? capture.substring(index + 1) : null;
        iterPtr.value = end;
        return new LURichTextTagInfo(name, attribute, isOpen, position);
    }

    private CharacterStyle styleFromValue(String value) {
        CharacterStyle style = colorStyleMap.get(value);
        if (style == null)
        {
            int color = colorFactory.fromValue(value);
            style = new ForegroundColorSpan(color);
            colorStyleMap.put(value, style);
        }
        return style;
    }

    private static class LURichTextTag
    {
        public final Object style;
        public final int startIndex;
        public final int length;

        public LURichTextTag(CharacterStyle style, int startIndex, int length) {
            this((Object) style, startIndex, length);
        }

        public LURichTextTag(StyleSpan style, int startIndex, int length) {
            this((Object) style, startIndex, length);
        }

        private LURichTextTag(Object style, int startIndex, int length) {
            this.style = style;
            this.startIndex = startIndex;
            this.length = length;
        }
    }

    private static final class LURichTextTagInfo
    {
        public final String name;
        public final String attribute;
        public final boolean open;
        public final int position;

        private LURichTextTagInfo(String name, String attribute, boolean open, int position) {
            this.name = name;
            this.attribute = attribute;
            this.open = open;
            this.position = position;
        }
    }

    //endregion
}
