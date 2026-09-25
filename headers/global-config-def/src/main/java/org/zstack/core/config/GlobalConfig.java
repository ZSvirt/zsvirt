package org.zstack.core.config;

import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;

/**
 * Definition-layer global config. Persistence / peer sync live in {@code GlobalConfigRuntime} (core).
 */
public class GlobalConfig implements ConfigDefinition {
    private String name;
    private String category;
    private String description;
    private String type;
    private String validatorRegularExpression;
    private String defaultValue;
    private volatile String value;
    private boolean linked;
    private transient List<GlobalConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private transient List<GlobalConfigBeforeUpdateExtensionPoint> beforeUpdateExtensions = new ArrayList<>();
    private transient List<GlobalConfigBeforeResetExtensionPoint> beforeResetExtensions = new ArrayList<>();
    private transient List<GlobalConfigValidatorExtensionPoint> validators = new ArrayList<>();
    private transient List<GlobalConfigQueryExtensionPoint> queryExtensions = new ArrayList<>();
    private transient List<GlobalConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private transient List<GlobalConfigBeforeUpdateExtensionPoint> localBeforeUpdateExtensions = new ArrayList<>();
    private GlobalConfigDef configDef;

    public GlobalConfig(String category, String name) {
        this.category = category;
        this.name = name;
    }

    GlobalConfig() {
    }

    public GlobalConfig asRuntime() {
        throw notRuntime("asRuntime");
    }

    /**
     * Whether Spring / EventFacade has been wired. Used by Facade link().
     */
    public boolean isSpringReady() {
        return false;
    }

    public GlobalConfig copy(GlobalConfig g) {
        setName(g.getName());
        setCategory(g.getCategory());
        setDescription(g.getDescription());
        setType(g.getType());
        setValidatorRegularExpression(g.getValidatorRegularExpression());
        setDefaultValue(g.getDefaultValue());
        setValue(g.value());
        setLinked(g.isLinked());

        validators = new ArrayList<>();
        queryExtensions = new ArrayList<>();
        updateExtensions = new ArrayList<>();
        localUpdateExtensions = new ArrayList<>();
        beforeUpdateExtensions = new ArrayList<>();

        updateExtensions.addAll(g.getUpdateExtensions());
        beforeUpdateExtensions.addAll(g.getBeforeUpdateExtensions());
        localUpdateExtensions.addAll(g.getLocalUpdateExtensions());
        beforeResetExtensions.addAll(g.getBeforeResetExtensions());
        validators.addAll(g.getValidators());
        queryExtensions.addAll(g.getQueryExtensions());
        localBeforeUpdateExtensions = new ArrayList<>(g.getLocalBeforeUpdateExtensions());
        configDef = g.getConfigDef();
        return this;
    }

    public void installLocalUpdateExtension(GlobalConfigUpdateExtensionPoint ext) {
        localUpdateExtensions.add(ext);
    }

    public void installUpdateExtension(GlobalConfigUpdateExtensionPoint ext) {
        updateExtensions.add(ext);
    }

    public void installBeforeResetExtension(GlobalConfigBeforeResetExtensionPoint ext) {
        beforeResetExtensions.add(ext);
    }

    /**
     * Common. Runs only on the writer node. Peer sync skips this hook.
     */
    public void installLocalBeforeUpdateExtension(GlobalConfigBeforeUpdateExtensionPoint ext) {
        localBeforeUpdateExtensions.add(ext);
    }

    /**
     * Uncommon. Prefer {@link #installLocalBeforeUpdateExtension} for write-intent guards.
     */
    public void installBeforeUpdateOnEveryNodeExtension(GlobalConfigBeforeUpdateExtensionPoint ext) {
        beforeUpdateExtensions.add(ext);
    }

    public void installValidateExtension(GlobalConfigValidatorExtensionPoint ext) {
        validators.add(ext);
    }

    public void installQueryExtension(GlobalConfigQueryExtensionPoint ext) {
        queryExtensions.add(ext);
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getValidatorRegularExpression() {
        return validatorRegularExpression;
    }

    public void setValidatorRegularExpression(String validatorRegularExpression) {
        this.validatorRegularExpression = validatorRegularExpression;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String value() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public <T> T value(Class<T> clz) {
        return TypeUtils.stringToValue(value(), clz);
    }

    public <T> T defaultValue(Class<T> clz) {
        return TypeUtils.stringToValue(defaultValue, clz);
    }

    public String getIdentity() {
        return produceIdentity(category, name);
    }

    public static String produceIdentity(String category, String name) {
        return String.format("%s.%s", category, name);
    }

    public void validate() {
        validate(value);
    }

    public void validate(String newValue) {
        for (GlobalConfigValidatorExtensionPoint ext : validators) {
            ext.validateGlobalConfig(category, name, value, newValue);
        }
    }

    public void normalize() {
        if (type == null) {
            return;
        }

        DebugUtils.Assert(value != null,
                String.format("value cannot be null, category: %s, name: %s", category, name));

        try {
            Class clz = Class.forName(type);
            Object v = TypeUtils.stringToValue(value, clz);
            value = v.toString();
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }
    }

    public GlobalConfigOptions getOptions() {
        GlobalConfigOptions configOptions = null;
        for (GlobalConfigQueryExtensionPoint ext : queryExtensions) {
            configOptions = ext.getConfigOptions();
        }
        return configOptions;
    }

    public boolean isLinked() {
        return linked;
    }

    public void setLinked(boolean linked) {
        this.linked = linked;
    }

    public boolean isMe(GlobalConfig other) {
        return category.equals(other.getCategory()) && name.equals(other.getName());
    }

    public GlobalConfigDef getConfigDef() {
        return configDef;
    }

    public void setConfigDef(GlobalConfigDef configDef) {
        this.configDef = configDef;
    }

    public String getCanonicalName() {
        return String.format("Global config[category: %s, name: %s]", category, name);
    }

    public void setValidators(List<GlobalConfigValidatorExtensionPoint> validators) {
        this.validators = validators;
    }

    public void setUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> updateExtensions) {
        this.updateExtensions = updateExtensions;
    }

    public void setLocalUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> localUpdateExtensions) {
        this.localUpdateExtensions = localUpdateExtensions;
    }

    public List<GlobalConfigValidatorExtensionPoint> getValidators() {
        return validators;
    }

    public List<GlobalConfigQueryExtensionPoint> getQueryExtensions() {
        return queryExtensions;
    }

    public List<GlobalConfigUpdateExtensionPoint> getUpdateExtensions() {
        return updateExtensions;
    }

    public List<GlobalConfigBeforeUpdateExtensionPoint> getBeforeUpdateExtensions() {
        return beforeUpdateExtensions;
    }

    public List<GlobalConfigUpdateExtensionPoint> getLocalUpdateExtensions() {
        return localUpdateExtensions;
    }

    public List<GlobalConfigBeforeUpdateExtensionPoint> getLocalBeforeUpdateExtensions() {
        return localBeforeUpdateExtensions;
    }

    public List<GlobalConfigBeforeResetExtensionPoint> getBeforeResetExtensions() {
        return beforeResetExtensions;
    }

    public void setBeforeResetExtensions(List<GlobalConfigBeforeResetExtensionPoint> beforeResetExtensions) {
        this.beforeResetExtensions = beforeResetExtensions;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(map(
                e("name", name),
                e("category", category),
                e("type", type),
                e("description", description),
                e("defaultValue", defaultValue),
                e("value", value),
                e("validatorRegularExpression", validatorRegularExpression)
        ));
    }

    // ---- runtime stubs (implemented by GlobalConfigRuntime) ----

    public void updateValue(Object val) {
        throw notRuntime("updateValue");
    }

    public void updateValueSkipValidation(Object val) {
        throw notRuntime("updateValueSkipValidation");
    }

    public void resetValue() {
        updateValue(defaultValue);
    }

    protected static IllegalStateException notRuntime(String method) {
        return new IllegalStateException(String.format(
                "GlobalConfig.%s requires GlobalConfigFacade init (GlobalConfigRuntime)", method));
    }
}
