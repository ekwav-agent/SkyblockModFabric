package com.coflnet.core;

import java.util.Locale;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class TradeValuation {
    public enum WorthBasis { LBIN, MEDIAN }

    private static final Pattern LBIN_VALUE = Pattern.compile(
            "\\b(?:lbin|lowest\\s*bin)\\s*:?\\s*~?\\s*([\\d,]+(?:\\.\\d+)?(?:\\s*[kmb]\\b)?)", Pattern.CASE_INSENSITIVE);
    private static final Pattern MED_VALUE = Pattern.compile(
            "\\bmed(?:ian)?\\s*:?\\s*~?\\s*([\\d,]+(?:\\.\\d+)?(?:\\s*[kmb]\\b)?)", Pattern.CASE_INSENSITIVE);

    private TradeValuation() {
    }

    public static Long parseWorthFromTips(String[] tips, WorthBasis basis) {
        if (tips == null) return null;
        Pattern label = basis == WorthBasis.LBIN ? LBIN_VALUE : MED_VALUE;
        Long bazaar = null;
        for (String tip : tips) {
            if (tip == null) continue;
            String plain = FormattingCodes.strip(tip).trim();
            Matcher matcher = label.matcher(plain);
            if (matcher.find()) {
                Long value = NumberParser.parseCoinNumber(matcher.group(1));
                if (value != null && value > 0) return value;
            }
            String lower = plain.toLowerCase(Locale.ROOT);
            if (bazaar == null && lower.contains("buy:") && lower.contains("each")) {
                bazaar = parseBazaarEach(plain, basis == WorthBasis.LBIN ? "buy" : "sell");
            }
        }
        return bazaar;
    }

    public static Long parseCoinOffer(String displayName) {
        if (displayName == null) return null;
        String plain = FormattingCodes.strip(displayName).trim();
        Matcher matcher = Pattern.compile("^([\\d,]*\\.?\\d+\\s*[kmb]?)\\s+coins$", Pattern.CASE_INSENSITIVE).matcher(plain);
        return matcher.matches() ? NumberParser.parseCoinNumber(matcher.group(1)) : null;
    }

    public static SideValue sum(List<Long> offeredValues) {
        long total = 0L;
        int unpriced = 0;
        for (Long value : offeredValues) {
            if (value == null) unpriced++;
            else total += value;
        }
        return new SideValue(total, unpriced);
    }

    private static Long parseBazaarEach(String plain, String side) {
        Matcher matcher = Pattern.compile(side + ":.*?\\(([\\d,.]+\\s*[kmb]?)\\s*each\\)", Pattern.CASE_INSENSITIVE).matcher(plain);
        return matcher.find() ? NumberParser.parseCoinNumber(matcher.group(1)) : null;
    }

    public record SideValue(long total, int unpriced) {
    }
}
