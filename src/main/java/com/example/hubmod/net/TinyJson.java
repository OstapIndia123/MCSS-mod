package com.example.hubmod.net;

import java.util.HashMap;
import java.util.Map;

public final class TinyJson {
    private TinyJson() {
    }

    // Очень простой парсер под формат объект с примитивами + вложенный объект ports
    public static Map<String, Object> parseObject(String json) {
        json = json.trim();
        if (json.startsWith("{")) json = json.substring(1);
        if (json.endsWith("}")) json = json.substring(0, json.length() - 1);

        Map<String, Object> out = new HashMap<>();

        // грубо: ищем "ports":{...} как сырой блок
        int portsIdx = json.indexOf("\"ports\":");
        if (portsIdx >= 0) {
            int brace = json.indexOf('{', portsIdx);
            int end = findMatchingBrace(json, brace);
            String portsRaw = json.substring(brace, end + 1);
            out.put("ports", parsePorts(portsRaw));

            // вырезаем ports чтобы не мешало
            String before = json.substring(0, portsIdx);
            String after = json.substring(end + 1);
            json = before + after;
        }

        for (String part : json.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int c = p.indexOf(':');
            if (c < 0) continue;
            String k = strip(p.substring(0, c));
            String v = p.substring(c + 1).trim();

            if (v.startsWith("\"")) out.put(k, strip(v));
            else if (v.matches("-?\\d+")) out.put(k, Integer.parseInt(v));
        }

        return out;
    }

    private static Map<String, Map<String, Object>> parsePorts(String portsJson) {
        // portsJson = { "north": { "mode":"OUT", "outputLevel":0 }, ... }
        Map<String, Map<String, Object>> ports = new HashMap<>();

        String s = portsJson.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        int i = 0;
        while (i < s.length()) {
            int q1 = s.indexOf('"', i);
            if (q1 < 0) break;
            int q2 = s.indexOf('"', q1 + 1);
            String name = s.substring(q1 + 1, q2);

            int brace = s.indexOf('{', q2);
            int end = findMatchingBrace(s, brace);
            String obj = s.substring(brace, end + 1);

            ports.put(name, parseFlatObject(obj));

            i = end + 1;
        }

        return ports;
    }

    private static Map<String, Object> parseFlatObject(String objJson) {
        String s = objJson.trim();
        if (s.startsWith("{")) s = s.substring(1);
        if (s.endsWith("}")) s = s.substring(0, s.length() - 1);

        Map<String, Object> m = new HashMap<>();
        for (String part : s.split(",")) {
            String p = part.trim();
            if (p.isEmpty()) continue;
            int c = p.indexOf(':');
            if (c < 0) continue;

            String k = strip(p.substring(0, c));
            String v = p.substring(c + 1).trim();

            if (v.startsWith("\"")) m.put(k, strip(v));
            else if (v.matches("-?\\d+")) m.put(k, Integer.parseInt(v));
        }
        return m;
    }

    private static int findMatchingBrace(String s, int start) {
        int depth = 0;
        for (int i = start; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (ch == '{') depth++;
            if (ch == '}') depth--;
            if (depth == 0) return i;
        }
        return s.length() - 1;
    }

    private static String strip(String s) {
        s = s.trim();
        if (s.startsWith("\"")) s = s.substring(1);
        if (s.endsWith("\"")) s = s.substring(0, s.length() - 1);
        return s;
    }
}
