package com.nikitagehlod.utils;

import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.events.WebDriverListener;

/**
 * Shows every automation step on screen while the tests run:
 *  - click  -> red outline on the element + red "ripple" circle where the click lands
 *  - typing -> blue outline on the field
 *
 * It is a WebDriver listener, so it catches EVERY element.click() / sendKeys() in the framework
 * (all page objects, old and new) without changing them. Switch it on/off in data.properties:
 *     show.clicks=true
 *     show.clicks.delayMs=400
 */
public class ClickHighlighter implements WebDriverListener {

    private static final String CLICK_COLOR = "#ff1744";
    private static final String TYPE_COLOR = "#2979ff";

    private final WebDriver rawDriver;
    private final long delayMs;

    public ClickHighlighter(WebDriver rawDriver, long delayMs) {
        this.rawDriver = rawDriver;
        this.delayMs = delayMs;
    }

    // ------------------------------------------------------------------ config

    public static boolean isEnabled() {
        String v = propertyReader.readKey("show.clicks");
        return v != null && v.trim().equalsIgnoreCase("true");
    }

    public static long configuredDelayMs() {
        String v = propertyReader.readKey("show.clicks.delayMs");
        try {
            return v == null ? 400 : Long.parseLong(v.trim());
        } catch (NumberFormatException e) {
            return 400;
        }
    }

    // --------------------------------------------------------- listener hooks

    @Override
    public void beforeClick(WebElement element) {
        mark(rawDriver, element, CLICK_COLOR, true, delayMs);
    }

    @Override
    public void beforeSendKeys(WebElement element, CharSequence... keysToSend) {
        // only outline the field, no pause - typing is already visible on screen
        mark(rawDriver, element, TYPE_COLOR, false, 0);
    }

    // ------------------------------------------------------------- the marker

    /**
     * Use this for actions the listener cannot see (JavaScript clicks, mouse hover).
     * Does nothing when show.clicks is off.
     */
    public static void showClick(WebDriver driver, WebElement element) {
        if (isEnabled()) {
            mark(driver, element, CLICK_COLOR, true, configuredDelayMs());
        }
    }

    private static final String SCRIPT =
            "var el = arguments[0], color = arguments[1], ripple = arguments[2];"
          + "if (!el || !el.getBoundingClientRect) return;"
          + "var r = el.getBoundingClientRect();"
          + "if (r.top < 0 || r.bottom > window.innerHeight) { el.scrollIntoView({block:'center'}); r = el.getBoundingClientRect(); }"
          // remember and set the outline
          + "var oldOutline = el.style.outline, oldOffset = el.style.outlineOffset;"
          + "el.style.outline = '3px solid ' + color; el.style.outlineOffset = '2px';"
          + "setTimeout(function(){ el.style.outline = oldOutline; el.style.outlineOffset = oldOffset; }, 900);"
          + "if (!ripple) return;"
          // one-time keyframes for the ripple
          + "if (!document.getElementById('qa-click-style')) {"
          + "  var st = document.createElement('style'); st.id = 'qa-click-style';"
          + "  st.textContent = '@keyframes qaClick{0%{transform:translate(-50%,-50%) scale(.3);opacity:1}"
          + "100%{transform:translate(-50%,-50%) scale(2.2);opacity:0}}';"
          + "  document.documentElement.appendChild(st);"
          + "}"
          // ripple at the element centre. Added to <html> (not <body>) so the 80% body zoom does not shift it.
          + "var d = document.createElement('div');"
          + "d.style.cssText = 'position:fixed;z-index:2147483647;pointer-events:none;width:34px;height:34px;"
          + "border-radius:50%;border:3px solid ' + color + ';background:' + color + '55;"
          + "animation:qaClick 700ms ease-out forwards;';"
          // keep the circle on the element even if the page shifts (lazy images loading above it)
          + "function place(){ var b = el.getBoundingClientRect();"
          + "  d.style.left = (b.left + b.width/2) + 'px'; d.style.top = (b.top + b.height/2) + 'px'; }"
          + "place(); document.documentElement.appendChild(d);"
          + "var iv = setInterval(place, 30);"
          + "setTimeout(function(){ clearInterval(iv); d.remove(); }, 800);";

    private static void mark(WebDriver driver, WebElement element, String color, boolean ripple, long pauseMs) {
        try {
            ((JavascriptExecutor) driver).executeScript(SCRIPT, element, color, ripple);
            if (pauseMs > 0) {
                Thread.sleep(pauseMs); // let the human see it before the click happens
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception ignored) {
            // highlighting must never fail a test (stale element, page navigating, ...)
        }
    }
}
