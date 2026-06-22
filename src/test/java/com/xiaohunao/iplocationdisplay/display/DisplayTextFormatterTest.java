package com.xiaohunao.iplocationdisplay.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayTextFormatterTest {
    private final DisplayTextFormatter formatter = new DisplayTextFormatter();

    @Test
    void insertsLocationIntoDisplayFormat() {
        assertEquals("[Guangdong]", formatter.format("[%location%]", "Guangdong", ""));
    }

    @Test
    void trimsFormattedDisplayText() {
        assertEquals("Guangdong", formatter.format(" %location% ", "Guangdong", ""));
    }

    @Test
    public void testFormatWithPlaytime() {
        DisplayTextFormatter formatter = new DisplayTextFormatter();
        String result = formatter.format("%location% %playtime%", "广东", "120h");
        assertEquals("广东 120h", result);
    }

    @Test
    public void testFormatLocationOnly() {
        DisplayTextFormatter formatter = new DisplayTextFormatter();
        String result = formatter.format("%location%", "广东", "120h");
        assertEquals("广东", result);
    }

    @Test
    public void testFormatWithEmptyPlaytime() {
        DisplayTextFormatter formatter = new DisplayTextFormatter();
        String result = formatter.format("%location% %playtime%", "广东", "");
        assertEquals("广东", result);
    }

    @Test
    public void testFormatWithNoPlaceholders() {
        DisplayTextFormatter formatter = new DisplayTextFormatter();
        String result = formatter.format("[固定文本]", "广东", "120h");
        assertEquals("[固定文本]", result);
    }
}
