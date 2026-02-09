package com.example.hubmod.net;

import java.util.Iterator;
import java.util.Map;

public final class Json {
    private Json() {
    }

    public static String minify(Map<String, Object> map) {
        StringBuilder sb = new StringBuilder();
        writeMap(sb, map);
        return sb.toString();
    }

    @SuppressWarnings("unchecked")
    private static void writeValue(StringBuilder sb, Object v) {
        if (v == null) {
            sb.append("null");
        } else if (v instanceof Number || v instanceof Boolean) {
            sb.append(v);
        } else if (v instanceof String) {
            sb.append('"').append(escape((String) v)).append('"');
        } else if (v instanceof Map) {
            writeMap(sb, (Map<String, Object>) v);
        } else {
            sb.append('"').append(escape(String.valueOf(v))).append('"');
        }
    }

    private static void writeMap(StringBuilder sb, Map<String, Object> map) {
        sb.append('{');
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            sb.append('"').append(escape(e.getKey())).append('"').append(':');
            writeValue(sb, e.getValue());
            if (it.hasNext()) sb.append(',');
        }
        sb.append('}');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
