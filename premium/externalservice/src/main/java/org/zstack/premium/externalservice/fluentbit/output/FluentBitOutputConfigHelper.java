package org.zstack.premium.externalservice.fluentbit.output;

import org.apache.commons.lang.StringUtils;
import org.zstack.header.identity.AccountConstant;

import java.util.LinkedHashMap;
import java.util.Map;

public class FluentBitOutputConfigHelper {
    public static Map<String, Object> makeOutputConfig(OutPutType type, Map<String, Object> configuration, String accountUuid) {
        configuration = normalizeConfiguration(configuration);
        LinkedHashMap<String, Object> map = new LinkedHashMap<>();

        switch (type) {
            case Forward:
                map.put("name", "forward");
                appendMatch(map, accountUuid);
                appendIfPresent(map, "host", configuration.get("host"));
                appendIfPresent(map, "port", configuration.get("port"));
                break;
            case Kafka:
                map.put("name", "kafka");
                appendMatch(map, accountUuid);
                appendIfPresent(map, "host", configuration.get("host"));
                appendIfPresent(map, "port", configuration.get("port"));
                appendIfPresent(map, "topics", configuration.get("topics"));
                break;
            case Syslog:
                map.put("name", "syslog");
                appendMatch(map, accountUuid);
                appendIfPresent(map, "host", configuration.get("host"));
                appendIfPresent(map, "port", configuration.get("port"));
                appendIfPresent(map, "mode", configuration.get("mode"));
                map.put("syslog_format", "rfc5424");
                map.put("syslog_maxsize", "2048");
                map.put("syslog_message_key", "platformOperationLog");
                break;
            case Elasticsearch:
                map.put("name", "es");
                appendMatch(map, accountUuid);
                appendIfPresent(map, "host", configuration.get("host"));
                appendIfPresent(map, "port", configuration.get("port"));
                appendIfPresent(map, "index", configuration.get("index"));
                map.put("suppress_type_name", "on");
                appendIfPresent(map, "http_user", firstNonNull(configuration.get("http_user"), configuration.get("httpUser")));
                appendIfPresent(map, "http_passwd", firstNonNull(configuration.get("http_passwd"), configuration.get("httpPassword")));
                Object esTls = configuration.get("tls");
                if (esTls != null && "on".equalsIgnoreCase(esTls.toString())) {
                    map.put("tls", esTls);
                    map.put("tls.verify", "off");
                }
                break;
            case Loki:
                map.put("name", "loki");
                appendMatch(map, accountUuid);
                appendIfPresent(map, "host", configuration.get("host"));
                appendIfPresent(map, "port", configuration.get("port"));
                appendIfPresent(map, "labels", configuration.get("labels"));
                map.put("line_format", "json");
                appendIfPresent(map, "http_user", firstNonNull(configuration.get("http_user"), configuration.get("httpUser")));
                appendIfPresent(map, "http_passwd", firstNonNull(configuration.get("http_passwd"), configuration.get("httpPassword")));
                Object lokiTls = configuration.get("tls");
                if (lokiTls != null && StringUtils.isNotEmpty(lokiTls.toString())) {
                    map.put("tls", lokiTls);
                    map.put("tls.verify", "off");
                }
                break;
            default:
                map.putAll(configuration);
        }

        return map;
    }

    public static Map<String, Object> normalizeConfiguration(Map<String, Object> configuration) {
        LinkedHashMap<String, Object> normalized = new LinkedHashMap<>();
        if (configuration == null) {
            return normalized;
        }

        normalized.putAll(configuration);
        copyIfMissing(normalized, "httpUser", "http_user", "username");
        copyIfMissing(normalized, "httpPassword", "http_passwd", "password");
        return normalized;
    }

    private static void appendMatch(Map<String, Object> map, String accountUuid) {
        if (StringUtils.isEmpty(accountUuid) || AccountConstant.INITIAL_SYSTEM_ADMIN_UUID.equals(accountUuid)) {
            map.put("match", "account_*");
        } else {
            map.put("match", "account_" + accountUuid);
        }
    }

    private static void appendIfPresent(Map<String, Object> map, String key, Object value) {
        if (isMeaningful(value)) {
            map.put(key, value);
        }
    }

    private static Object firstNonNull(Object first, Object second) {
        return isMeaningful(first) ? first : second;
    }

    private static void copyIfMissing(Map<String, Object> map, String target, String... aliases) {
        if (isMeaningful(map.get(target))) {
            return;
        }

        for (String alias : aliases) {
            Object value = map.get(alias);
            if (isMeaningful(value)) {
                map.put(target, value);
                return;
            }
        }
    }

    private static boolean isMeaningful(Object value) {
        return value != null && StringUtils.isNotEmpty(value.toString()) && !"null".equalsIgnoreCase(value.toString());
    }
}
