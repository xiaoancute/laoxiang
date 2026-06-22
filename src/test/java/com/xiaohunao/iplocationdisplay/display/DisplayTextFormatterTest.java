package com.xiaohunao.iplocationdisplay.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DisplayTextFormatterTest {
    private final DisplayTextFormatter formatter = new DisplayTextFormatter();

    @Test
    void insertsLocationIntoDisplayFormat() {
        assertEquals("[Guangdong]", formatter.format("[%location%]", "Guangdong"));
    }

    @Test
    void trimsFormattedDisplayText() {
        assertEquals("Guangdong", formatter.format(" %location% ", "Guangdong"));
    }
}
