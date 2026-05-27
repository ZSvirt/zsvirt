package org.zstack.premium.externalservice.fluentbit;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FluentBitServiceUnitConfig {
    public static final String serviceName = "fluent-bit";
    public static final String serviceUnitPath = String.format("/usr/lib/systemd/system/%s.service", serviceName);
    private static final String basePath = "/var/lib/zstack/fluent-bit/";
    public static final String configFilePath = basePath + "config/fluent-bit.conf";
    public static final String externalConfigFileName = "fluent-bit-external.conf";
    public static String externalConfigFilePath = String.format("%s/config/%s", basePath, externalConfigFileName);
    public static final String secretsFilePath = basePath + "config/fluent-bit-secrets.env";
    public static final String binPath = basePath + "bin/fluent-bit";
    public static final String logFilePath = basePath + "fluent-bit-server.log";

    private static final CLogger logger = Utils.getLogger(FluentBitServiceUnitConfig.class);

    public static String makeServiceUnitConfig() {
        StringBuilder builder = new StringBuilder();

        builder.append("[Unit]\n");
        builder.append("Description=Fluent Bit\n");
        builder.append("Documentation=https://docs.fluentbit.io/manual/\n");
        builder.append("Requires=network.target\n");
        builder.append("After=network.target\n\n");

        builder.append("[Service]\n");
        builder.append("Type=simple\n");
        builder.append("User=zstack\n");
        builder.append(String.format("EnvironmentFile=-%s\n", secretsFilePath));
        builder.append(String.format("ExecStart=%s -c %s\n", binPath, configFilePath));
        builder.append("StartLimitInterval=60s\n");
        builder.append("StartLimitBurst=10\n");
        builder.append("Restart=always\n");
        builder.append("LimitNOFILE=262144\n\n");

        builder.append("[Install]\n");
        builder.append("WantedBy=multi-user.target\n");

        return builder.toString();
    }

    public static boolean isInstalled() {
        return PathUtil.exists(binPath);
    }

    public static boolean checkForSection(String filePath, String section) {
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.trim().equals(section)) {
                    return true;
                }
            }
        } catch (IOException e) {
            logger.warn(String.format("failed to check section in %s: %s", filePath, e.getMessage()));
        }
        return false;
    }

    public static void generateFluentBitConfig() {
        Map<String, Object> configMap = generateFluentBitConfigMap();
        writeFluentBitConfig(configMap, configFilePath);
    }

    private static Map<String, Object> generateFluentBitConfigMap() {
        LinkedHashMap<String, Object> fluentBitConfigMap = new LinkedHashMap<>();
        // [SERVICE] section
        LinkedHashMap<String, Object> serviceConfigMap = new LinkedHashMap<>();
        serviceConfigMap.put("flush", 1);
        serviceConfigMap.put("daemon", "Off");
        serviceConfigMap.put("log_level", "info");
        serviceConfigMap.put("log_file", logFilePath);
        serviceConfigMap.put("parsers_file", "parsers.conf");
        serviceConfigMap.put("plugins_file", "plugins.conf");
        fluentBitConfigMap.put("[SERVICE]", serviceConfigMap);

        // [INPUT] section
        LinkedHashMap<String, Object> inputConfigMap = new LinkedHashMap<>();
        inputConfigMap.put("name", "tail");
        inputConfigMap.put("tag", "zstack.operator.log");
        inputConfigMap.put("path", String.format("%s/zstack-ui/logs/zstack-ui-server.log", System.getProperty("user.home")));
        inputConfigMap.put("parser", "json");
        fluentBitConfigMap.put("[INPUT]", inputConfigMap);

        // [FILTER] sections
        List<Map<String, Object>> filterConfigList = new ArrayList<>();

        LinkedHashMap<String, Object> filterConfigMap1 = new LinkedHashMap<>();
        filterConfigMap1.put("name", "grep");
        filterConfigMap1.put("match", "zstack.operator.log");
        filterConfigMap1.put("regex", "type operationLog");
        filterConfigList.add(filterConfigMap1);

        LinkedHashMap<String, Object> filterConfigMap2 = new LinkedHashMap<>();
        filterConfigMap2.put("name", "rewrite_tag");
        filterConfigMap2.put("match", "zstack.operator.log");
        filterConfigMap2.put("rule", "$accountUuid .* account_$accountUuid false");
        filterConfigList.add(filterConfigMap2);

        LinkedHashMap<String, Object> filterConfigMap3 = new LinkedHashMap<>();
        filterConfigMap3.put("name", "lua");
        filterConfigMap3.put("match", "account_*");
        filterConfigMap3.put("script", "syslog.lua");
        filterConfigMap3.put("call", "convert_json_to_string");
        filterConfigList.add(filterConfigMap3);
        fluentBitConfigMap.put("[FILTER]", filterConfigList);

        return fluentBitConfigMap;
    }

    private static void writeFluentBitConfig(Map<String, Object> configMap, String filePath){
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            // write include configurations
            writer.write(String.format("@INCLUDE %s", externalConfigFileName));
            writer.newLine();
            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String section = entry.getKey();
                Object sectionConfig = entry.getValue();

                if (sectionConfig instanceof List) {
                    // Handle multiple [FILTER] sections
                    for (Map<String, Object> filterConfig : (List<Map<String, Object>>) sectionConfig) {
                        writer.write(section);
                        writer.newLine();
                        for (Map.Entry<String, Object> configEntry : filterConfig.entrySet()) {
                            String key = configEntry.getKey();
                            Object value = configEntry.getValue();
                            writer.write("    " + key + " " + value);
                            writer.newLine();
                        }
                        writer.newLine();
                    }
                } else if (sectionConfig instanceof Map) {
                    // Handle single section
                    writer.write(section);
                    writer.newLine();
                    for (Map.Entry<String, Object> configEntry : ((Map<String, Object>) sectionConfig).entrySet()) {
                        String key = configEntry.getKey();
                        Object value = configEntry.getValue();
                        writer.write("    " + key + " " + value);
                        writer.newLine();
                    }
                    writer.newLine();
                }
            }
        } catch (IOException e) {
            logger.warn(String.format("failed to write fluent-bit config: %s", e.getMessage()));
        }
    }

    public static void addSingleFluentBitConfig(Map<String, Object> outputMap) {
        if (outputMap.containsKey("http_passwd")) {
            String match = (String) outputMap.get("match");
            String envKey = "FB_HTTP_PASSWD_" + match.replaceAll("[^A-Za-z0-9]", "_").toUpperCase();
            writeSecret(envKey, (String) outputMap.get("http_passwd"));
            outputMap = new java.util.LinkedHashMap<>(outputMap);
            outputMap.put("http_passwd", "${" + envKey + "}");
        }
        appendFluentBitConfigFile(configFilePath, outputMap);
    }

    private static void writeSecret(String key, String value) {
        try (java.io.PrintWriter pw = new java.io.PrintWriter(new FileWriter(secretsFilePath, true))) {
            pw.println(key + "=" + value);
        } catch (IOException e) {
            logger.warn(String.format("failed to write fluent-bit secret: %s", e.getMessage()));
        }
    }

    private static void appendFluentBitConfigFile(String filePath, Map<String, Object> configMap) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath, true))) {
            writer.write("[OUTPUT]");
            writer.newLine();

            boolean isKafka = "kafka".equals(configMap.get("name"));
            String host = null;
            String port = null;

            for (Map.Entry<String, Object> entry : configMap.entrySet()) {
                String key = entry.getKey();
                String value = (String) entry.getValue();
                if (isKafka && "host".equals(key)) {
                    host = value;
                    continue;
                }
                if (isKafka && "port".equals(key)) {
                    port = value;

                    if (host != null && port != null) {
                        String brokers = String.format("%s:%s", host, port);
                        writer.write("    brokers " + brokers);
                        writer.newLine();
                    }
                    continue;
                }

                writer.write("    " + key + " " + value);
                writer.newLine();
            }

            writer.newLine();
        } catch (IOException e) {
            logger.warn(String.format("failed to write fluent-bit config: %s", e.getMessage()));
        }
    }

    public static void removeOutputConfig(String configuration) {
        String configContent = readFile(configFilePath);
        Pattern pattern = Pattern.compile(convertJsonToRegex(configuration), Pattern.MULTILINE);
        String updatedContent = removeBlock(configContent, pattern);
        writeFile(configFilePath, updatedContent);
    }

    public static String convertJsonToRegex(String jsonString) {
        Map<String, String> jsonMap = JSONObjectUtil.toObject(jsonString, Map.class);

        StringBuilder regexBuilder = new StringBuilder();
        regexBuilder.append("\\[OUTPUT\\]\\s*\n");
        boolean isKafka = "kafka".equals(jsonMap.get("name"));
        String host = null;
        String port = null;

        for (Map.Entry<String, String> entry : jsonMap.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if (isKafka && "host".equals(key)) {
                host = value;
                continue;
            }
            if (isKafka && "port".equals(key)) {
                port = value;

                if (host != null && port != null) {
                    String brokers = String.format("%s:%s", host, port);
                    regexBuilder.append("\\s*brokers\\s+").append(escapeSpecialChars(brokers)).append("\\s*\n");
                }
                continue;
            }
            regexBuilder.append("\\s*").append(key).append("\\s+").append(escapeSpecialChars(value)).append("\\s*\n");
        }

        return regexBuilder.toString();
    }

    private static String escapeSpecialChars(String input) {
        return input.replaceAll("([.*+?^${}()|\\[\\]\\\\])", "\\\\$1");
    }

    private static String readFile(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (Exception e) {
            logger.warn(String.format("failed to read file: %s", e.getMessage()));
        }
        return content.toString();
    }

    private static String removeBlock(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        // delete the first matched [OUTPUT]
        if (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();

            return content.substring(0, start) + content.substring(end);
        }
        return content;
    }

    private static void writeFile(String filePath, String content) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(content);
        } catch (Exception e) {
            logger.warn(String.format("failed to write file %s: %s", filePath, e.getMessage()));
        }
    }

    public static void generateExternalConfig(List<Map<String, Object>> invs){
        logger.info("start to generate fluent bit external config");
        if (invs == null || invs.isEmpty()) {
            writeFile(externalConfigFilePath, "");
            return;
        }

        String extConfig = parseExternalConfig(invs);
        writeFile(externalConfigFilePath, extConfig);
    }

    public static String parseExternalConfig(List<Map<String, Object>> invs){
        StringBuilder sb = new StringBuilder();
        ObjectMapper mapper = new ObjectMapper();
        invs.forEach(inv -> {
            String uuid = String.valueOf(inv.get("uuid"));
            String serviceType = String.valueOf(inv.get("serviceType"));
            Object configurationObj = inv.get("configuration");
            if (configurationObj == null || configurationObj.toString().trim().isEmpty()) {
                logger.warn(String.format("external configuration[uuid:%s][type:%s] contains empty configuration, skip", uuid, serviceType));
                return;
            }

            Map<String, Object> pipeline;
            try {
                pipeline = mapper.readValue(configurationObj.toString(), new TypeReference<Map<String, Object>>(){});
            } catch (Exception e) {
                logger.error(String.format("external configuration[uuid:%s][type:%s] content failed to parse: %s, skip", uuid, serviceType, e));
                return;
            }

            Object inputObj  = pipeline.get("inputs");
            Object outputObj = pipeline.get("outputs");
            Object filterObj = pipeline.get("filters");

            if (inputObj instanceof List) {
                for (Object o : (List<?>) inputObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> input = (Map<String, Object>) o;
                        renderInput(sb, input);
                    } else {
                        logger.warn(String.format("external configuration[uuid:%s][type:%s] contains invalid input, skip", uuid, serviceType));
                    }
                }
            } else {
                logger.warn(String.format("external configuration[uuid:%s][type:%s] contains no input, skip", uuid, serviceType));
            }

            if (filterObj instanceof List) {
                for (Object o: (List<?>) filterObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> filter = (Map<String, Object>) o;
                        renderFilter(sb, filter);
                    } else {
                        logger.warn(String.format("external configuration[uuid:%s][type:%s] contains invalid filter, skip", uuid, serviceType));
                    }
                }
            }

            if (outputObj instanceof List) {
                for (Object o : (List<?>) outputObj) {
                    if (o instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> output = (Map<String, Object>) o;
                        renderOutput(sb, output);
                    } else {
                        logger.warn(String.format("external configuration[uuid:%s][type:%s] contains invalid output, skip", uuid, serviceType));
                    }
                }
            } else {
                logger.warn(String.format("external configuration[uuid:%s][type:%s] contains no output, skip", uuid, serviceType));
            }

        });

        return sb.toString();
    }

    public static void renderInput(StringBuilder sb, Map<String, Object> input) {
        if (input == null || input.isEmpty() ) {return;}

        Object nameObj = input.get("name");
        if (!(nameObj instanceof String)) {return;}

        String name = nameObj.toString().trim();

        switch (name) {
            case "tail":
                renderTailInput(sb, input);
                break;
            default:
                logger.warn(String.format("external configuration contains unsupported input type: %s, skip", name));
        }
    }

    public static void renderFilter(StringBuilder sb, Map<String, Object> filter) {
        if (filter == null || filter.isEmpty()) {return;}

        Object nameObj = filter.get("name");
        Object matchObj = filter.get("match");
        if (!(nameObj instanceof String) || ((String) nameObj).trim().isEmpty()) {return;}
        if (!(matchObj instanceof String) || ((String) matchObj).trim().isEmpty()) {return;}

        sb.append("[FILTER]\n");
        sb.append("    Name   ").append(((String) nameObj).trim()).append("\n");
        sb.append("    Match   ").append(((String) matchObj).trim()).append("\n");

        for (Map.Entry<String, Object> entry: filter.entrySet()) {
            String key = entry.getKey();
            if ("name".equalsIgnoreCase(key) || "match".equalsIgnoreCase((key))) continue;

            Object value = entry.getValue();
            if (value == null) {
                continue;
            }
            if (value instanceof List) {
                for (Object v : (List<?>) value) {
                    if (v != null) {
                        sb.append("    ").append(key).append("   ").append(v.toString()).append("\n");
                    }
                }
            } else {
                sb.append("    ").append(key).append("   ").append(value.toString()).append("\n");
            }
        }

        sb.append("\n");
    }

    public static void renderOutput(StringBuilder sb, Map<String, Object> output) {
        if (output == null || output.isEmpty()) {return;}

        Object nameObj = output.get("name");
        if (!(nameObj instanceof String)) {return;}

        String name = nameObj.toString().trim();

        switch (name) {
            case "http":
                renderHttpOutput(sb, output);
                break;
            default:
                logger.warn(String.format("external configuration contains unsupported output type: %s, skip", name));
        }
    }

    public static void renderTailInput(StringBuilder sb, Map<String, Object> input) {
        Object tagObj = input.get("tag");
        Object pathObj = input.get("path");

        if (!(tagObj instanceof String) || ((String) tagObj).trim().isEmpty()) {return;}
        if (!(pathObj instanceof String) || ((String) pathObj).trim().isEmpty()) {return;}

        sb.append("[INPUT]\n");
        sb.append("    Name   tail\n");
        sb.append("    path   ").append(((String) pathObj).trim()).append('\n');
        sb.append("    tag   ").append(((String) tagObj).trim()).append('\n');

        appendIfPresent(sb, "read_from_head", input.get("read_from_head"));

        sb.append('\n');
    }

    public static void renderHttpOutput(StringBuilder sb, Map<String, Object> output) {
        Object matchObj = output.get("match");
        Object hostObj = output.get("host");
        Object portObj = output.get("port");

        if (!(matchObj instanceof String) || ((String) matchObj).trim().isEmpty()) {
            return;
        }
        if (!(hostObj instanceof String) || ((String) hostObj).trim().isEmpty()) {
            return;
        }

        if (portObj == null) {
            return;
        }
        String port = portObj.toString().trim();
        if (port.isEmpty() || !port.matches("\\d+")) {
            logger.warn(String.format("external configuration contains invalid port: %s, skip", portObj));
            return;
        }

        sb.append("[OUTPUT]\n");
        sb.append("    Name    http\n");
        sb.append("    Match   ").append(((String) matchObj).trim()).append('\n');
        sb.append("    host    ").append(((String) hostObj).trim()).append('\n');
        sb.append("    port    ").append(portObj).append('\n');

        appendIfPresent(sb, "uri", output.get("uri"));
        appendIfPresent(sb, "format", output.get("format"));
        appendIfPresent(sb, "json_date_format", output.get("json_date_format"));

        Object headerObj = output.get("header");
        if (headerObj instanceof List) {
            for (Object h : (List<?>) headerObj) {
                if (h != null) {
                    sb.append("    header  ").append(h.toString()).append('\n');
                }
            }
        }

        appendIfPresent(sb, "http_user", output.get("http_user"));
        appendIfPresent(sb, "http_passwd", output.get("http_passwd"));

        sb.append('\n');
    }

    private static void appendIfPresent(StringBuilder sb, String key, Object value) {
        if (value == null) {
            return;
        }
        String v = value.toString().trim();
        if (!v.isEmpty()) {
            sb.append("    ").append(key).append("  ").append(v).append('\n');
        }
    }
}
