package uz.zarflow.humo;

import java.util.regex.Pattern;

/**
 * Recognizes a single line that looks like it belongs to a HUMO transaction/balance
 * notification but doesn't necessarily contain a whole one by itself -- @HUMOcardbot
 * forwards routinely split one notification across several consecutive Telegram
 * messages (one per line: 💸, ➖, 📍, 💳, 🕓 each arrive separately). The bot layer uses
 * this to decide whether an unparsed line should be buffered and merged with its
 * neighbors (see {@code uz.zarflow.telegram.HumoFragmentBuffer}) rather than falling
 * straight through to quick-input, where a bare card/time line would otherwise be
 * misread as a manual amount entry.
 */
public final class HumoFragmentDetector {

    private static final Pattern MARKER = Pattern.compile("[💸🎉➖➕📍💳🕓🔹💵]|(?i)humocard");

    private HumoFragmentDetector() {
    }

    public static boolean looksLikeFragment(String text) {
        return text != null && MARKER.matcher(text).find();
    }

}
