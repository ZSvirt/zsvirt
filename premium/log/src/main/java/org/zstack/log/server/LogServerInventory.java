package org.zstack.log.server;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Inventory(mappingVOClass = LogServerVO.class)
@PythonClassInventory
public class LogServerInventory implements Serializable {
    private static final Set<String> SENSITIVE_CONFIGURATION_KEYS = new HashSet<>(Arrays.asList(
            "password",
            "http_passwd",
            "httpPassword"
    ));
    private static final Pattern PASSWORD_PATTERN = Pattern.compile("(\\\"(?:password|http_passwd|httpPassword)\\\"\\s*:\\s*\\\")([^\\\"]*)(\\\")");
    private String uuid;
    private String name;
    private String description;
    private LogCategory category;
    private LogType type;
    private LogLevel level;
    private LogServerState state;
    private String configuration;
    private String accountUuid;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public static LogServerInventory valueOf(LogServerVO vo) {
        LogServerInventory inv = rawValueOf(vo);
        inv.setConfiguration(maskSensitiveConfiguration(inv.getConfiguration()));
        return inv;
    }

    public static LogServerInventory rawValueOf(LogServerVO vo) {
        LogServerInventory inv = new LogServerInventory();
        inv.setUuid(vo.getUuid());
        inv.setName(vo.getName());
        inv.setDescription(vo.getDescription());
        inv.setCategory(vo.getCategory());
        inv.setType(vo.getType());
        inv.setLevel(vo.getLevel());
        inv.setState(vo.getState());
        inv.setConfiguration(vo.getConfiguration());
        inv.setAccountUuid(vo.getAccountUuid());
        inv.setCreateDate(vo.getCreateDate());
        inv.setLastOpDate(vo.getLastOpDate());
        return inv;
    }

    public static List<LogServerInventory> valueOf(Collection<LogServerVO> vos) {
        List<LogServerInventory> invs = new ArrayList<>();
        for (LogServerVO vo : vos) {
            invs.add(valueOf(vo));
        }
        return invs;
    }

    public static String maskSensitiveConfiguration(String conf) {
        if (conf == null) {
            return null;
        }

        try {
            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(conf);
            maskSensitiveNode(root, mapper);
            return mapper.writeValueAsString(root);
        } catch (Exception e) {
            return PASSWORD_PATTERN.matcher(conf).replaceAll("$1***$3");
        }
    }

    private static void maskSensitiveNode(JsonNode node, ObjectMapper mapper) {
        if (node == null || node.isNull()) {
            return;
        }

        if (node.isObject()) {
            ObjectNode objectNode = (ObjectNode) node;
            Iterator<Map.Entry<String, JsonNode>> fields = objectNode.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String fieldName = entry.getKey();
                JsonNode value = entry.getValue();

                if (SENSITIVE_CONFIGURATION_KEYS.contains(fieldName)) {
                    objectNode.put(fieldName, "***");
                    continue;
                }

                if (value != null && value.isTextual()) {
                    objectNode.put(fieldName, maskSensitiveText(value.asText(), mapper));
                    continue;
                }

                maskSensitiveNode(value, mapper);
            }
            return;
        }

        if (node.isArray()) {
            ArrayNode arrayNode = (ArrayNode) node;
            for (JsonNode child : arrayNode) {
                maskSensitiveNode(child, mapper);
            }
        }
    }

    private static String maskSensitiveText(String text, ObjectMapper mapper) {
        if (text == null) {
            return null;
        }

        try {
            JsonNode root = mapper.readTree(text);
            maskSensitiveNode(root, mapper);
            return mapper.writeValueAsString(root);
        } catch (Exception ignored) {
            return PASSWORD_PATTERN.matcher(text).replaceAll("$1***$3");
        }
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LogCategory getCategory() {
        return category;
    }

    public void setCategory(LogCategory category) {
        this.category = category;
    }

    public LogType getType() {
        return type;
    }

    public void setType(LogType type) {
        this.type = type;
    }

    public LogLevel getLevel() {
        return level;
    }

    public void setLevel(LogLevel level) {
        this.level = level;
    }

    public String getConfiguration() {
        return configuration;
    }

    public void setConfiguration(String configuration) {
        this.configuration = configuration;
    }

    public LogServerState getState() {
        return state;
    }

    public void setState(LogServerState state) {
        this.state = state;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
