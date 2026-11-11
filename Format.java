package com.paytracker;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Format {
    public static String money(double v) {
        String sign = v < 0 ? "-" : "";
        v = Math.abs(v);
        if (v >= 1e12) return sign + "$" + trim(v / 1e12) + "T";
        if (v >= 1e9)  return sign + "$" + trim(v / 1e9) + "B";
        if (v >= 1e6)  return sign + "$" + trim(v / 1e6) + "M";
        if (v >= 1e3)  return sign + "$" + trim(v / 1e3) + "K";
        return sign + "$" + trim(v);
    }

    public static String signed(double v) {
        return (v >= 0 ? "+" : "-") + money(Math.abs(v));
    }

    private static String trim(double v) {
        String s = String.format("%.2f", v);
        if (s.contains(".")) s = s.replaceAll("0+$", "").replaceAll("\\.$", "");
        return s;
    }

    public static String time(long ms) {
        long sec = (ms + 999) / 1000;
        return String.format("%02d:%02d", sec / 60, sec % 60);
    }

    public static double mult(String suf) {
        if (suf == null || suf.isEmpty()) return 1;
        switch (Character.toLowerCase(suf.charAt(0))) {
            case 'k': return 1e3;
            case 'm': return 1e6;
            case 'b': return 1e9;
            case 't': return 1e12;
            default: return 1;
        }
    }

    private static final Pattern AMOUNT = Pattern.compile("^\\$?\\s*([\\d,]+(?:\\.\\d+)?)\\s*([kKmMbBtT])?$");

    /** Parses things like 500, 1.5K, $452B. Returns -1 if invalid. */
    public static double parseAmount(String in) {
        if (in == null) return -1;
        Matcher m = AMOUNT.matcher(in.trim());
        if (!m.matches()) return -1;
        try { return Double.parseDouble(m.group(1).replace(",", "")) * mult(m.group(2)); }
        catch (NumberFormatException e) { return -1; }
    }

    /** Parses 30, 45s, 2m, 1:30. Returns seconds or -1 if invalid. */
    public static int parseSeconds(String in) {
        if (in == null) return -1;
        String s = in.trim().toLowerCase();
        if (s.isEmpty()) return -1;
        try {
            long sec;
            if (s.contains(":")) {
                String[] p = s.split(":");
                if (p.length != 2) return -1;
                sec = Long.parseLong(p[0].trim()) * 60 + Long.parseLong(p[1].trim());
            } else if (s.endsWith("m")) {
                sec = Long.parseLong(s.substring(0, s.length() - 1).trim()) * 60;
            } else if (s.endsWith("s")) {
                sec = Long.parseLong(s.substring(0, s.length() - 1).trim());
            } else {
                sec = Long.parseLong(s);
            }
            if (sec < 1 || sec > 86400) return -1;
            return (int) sec;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static final String SC_FROM = "\u1D00\u0299\u1D04\u1D05\u1D07\uA730\u0262\u029C\u026A\u1D0A\u1D0B\u029F\u1D0D\u0274\u1D0F\u1D18\u01EB\u0280\uA731\u1D1B\u1D1C\u1D20\u1D21x\u028F\u1D22";
    private static final String SC_TO = "abcdefghijklmnopqrstuvwxyz";

    /** Turns fancy fonts (full-width, small caps) into plain ASCII so patterns can match. */
    public static String normalize(String s) {
        String n = Normalizer.normalize(s, Normalizer.Form.NFKC);
        StringBuilder sb = new StringBuilder(n.length());
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            int idx = SC_FROM.indexOf(c);
            sb.append(idx >= 0 ? SC_TO.charAt(idx) : c);
        }
        return sb.toString();
    }
}
