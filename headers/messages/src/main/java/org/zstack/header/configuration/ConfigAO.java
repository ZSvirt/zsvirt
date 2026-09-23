package org.zstack.header.configuration;

import org.springframework.lang.NonNull;
import org.zstack.utils.Tags;

import java.util.Map;
import java.util.Objects;

@PythonClassInventory
public class ConfigAO {
    public static final String RESOURCE_CONFIG_TAG_PREFIX = "resourceConfig";
    public static final String RESOURCE_CONFIG_CATEGORY_TOKEN = "category";
    public static final String RESOURCE_CONFIG_NAME_TOKEN = "name";
    public static final String RESOURCE_CONFIG_VALUE_TOKEN = "value";

    public static final String TAG_FORMAT = Tags.join(
                RESOURCE_CONFIG_TAG_PREFIX,
                Tags.placeholder(RESOURCE_CONFIG_CATEGORY_TOKEN),
                Tags.placeholder(RESOURCE_CONFIG_NAME_TOKEN),
                Tags.placeholder(RESOURCE_CONFIG_VALUE_TOKEN));

    private String category;
    private String name;
    private String value;

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public static boolean match(@NonNull String tag) {
        return tag.startsWith(RESOURCE_CONFIG_TAG_PREFIX);
    }

    public static ConfigAO of(@NonNull String tag) {
        Map<String, String> tokens = Tags.parseIfMatch(TAG_FORMAT, tag);
        Objects.requireNonNull(tokens, () -> "invalid resource config tag: " + tag);

        ConfigAO config = new ConfigAO();
        config.setCategory(Objects.requireNonNull(tokens.get(RESOURCE_CONFIG_CATEGORY_TOKEN),
                () -> "invalid category in resource config tag: " + tag));
        config.setName(Objects.requireNonNull(tokens.get(RESOURCE_CONFIG_NAME_TOKEN),
                () -> "invalid name in resource config tag: " + tag));
        config.setValue(Objects.requireNonNull(tokens.get(RESOURCE_CONFIG_VALUE_TOKEN),
                () -> "invalid value in resource config tag: " + tag));
        return config;
    }
}
