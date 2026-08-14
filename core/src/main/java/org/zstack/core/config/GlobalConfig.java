package org.zstack.core.config;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfigCanonicalEvents.UpdateEvent;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.function.ForEachFunction;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GlobalConfig {
    private static final CLogger logger = Utils.getLogger(GlobalConfig.class);

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

    private static Map<String, String> propertiesMap = new HashMap<>();
    static {
        boolean noTrim = System.getProperty("DoNotTrimPropertyFile") != null;
        for (final String name : System.getProperties().stringPropertyNames()) {
            String value = System.getProperty(name);
            if (!noTrim) {
                value = value.trim();
            }
            propertiesMap.put(name, value);
        }
    }

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;

    public EventFacade getEvtf() {
        return evtf;
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

    public void normalize() {
        if (type == null) {
            // means String, no need to normalize
            return;
        }

        // for bug: http://dev.zstack.io/browse/ZSTAC-8753
        DebugUtils.Assert(value != null, String.format("value cannot be null, category: %s, name: %s", category, name));

        try {
            Class clz = Class.forName(type);
            Object v = TypeUtils.stringToValue(value, clz);
            value = v.toString();
        } catch (ClassNotFoundException e) {
            throw new CloudRuntimeException(e);
        }
    }

    public GlobalConfig(String category, String name) {
        this.category = category;
        this.name = name;
    }

    GlobalConfig() {
    }

    GlobalConfig copy(GlobalConfig g){
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
        configDef = g.getConfigDef();
        return this;
    }

    private String makeUpdateEventPath() {
        return s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", category),
                e("name", name)
        ));
    }

    public GlobalConfigVO reload() {
        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, category);
        q.add(GlobalConfigVO_.name, Op.EQ, name);
        return q.find();
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
     * Common. Runs only on the writer node. Peer sync skips this hook, so write-intent
     * checks and one-shot side effects are not repeated (for example, reject enabling an
     * already enabled feature, or encrypt passwords once when toggling password encryption).
     *
     * <pre>
     * writer (localUpdate=true):
     *   validate
     *   -> beforeUpdateOnEveryNode
     *   -> localBeforeUpdate                &lt;&lt;-- this method
     *   -> update in-memory cache
     *   -> update DB
     *   -> localUpdateExtension
     *   -> updateExtension
     *   -> event to peer
     *
     * peer (localUpdate=false):
     *   load value from DB
     *   -> validate
     *   -> beforeUpdateOnEveryNode
     *   -> (skip localBeforeUpdate)         &lt;&lt;-- this method does NOT run
     *   -> update in-memory cache
     *   -> (skip DB write)
     *   -> (skip localUpdateExtension)
     *   -> updateExtension
     * </pre>
     */
    public void installLocalBeforeUpdateExtension(GlobalConfigBeforeUpdateExtensionPoint ext) {
        localBeforeUpdateExtensions.add(ext);
    }

    /**
     * Uncommon. Prefer {@link #installLocalBeforeUpdateExtension} for write-intent guards
     * and one-shot side effects. Use this only when every management node must re-check or
     * prepare its own local state before adopting the new value (for example, validate that
     * this node's log4j XML can accept a new log-retention setting). Throwing on the peer
     * path can leave that node's in-memory value out of date with the database.
     *
     * <pre>
     * writer (localUpdate=true):
     *   validate
     *   -> beforeUpdateOnEveryNode          &lt;&lt;-- this method
     *   -> localBeforeUpdate
     *   -> update in-memory cache
     *   -> update DB
     *   -> localUpdateExtension
     *   -> updateExtension
     *   -> event to peer
     *
     * peer (localUpdate=false):
     *   load value from DB
     *   -> validate
     *   -> beforeUpdateOnEveryNode          &lt;&lt;-- this method (runs again)
     *   -> (skip localBeforeUpdate)
     *   -> update in-memory cache
     *   -> (skip DB write)
     *   -> (skip localUpdateExtension)
     *   -> updateExtension
     * </pre>
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

    public String getName() {
        return name;
    }

    void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    void setCategory(String category) {
        this.category = category;
    }

    public String getDescription() {
        return description;
    }

    void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    void setType(String type) {
        this.type = type;
    }

    public String getValidatorRegularExpression() {
        return validatorRegularExpression;
    }

    void setValidatorRegularExpression(String validatorRegularExpression) {
        this.validatorRegularExpression = validatorRegularExpression;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String value() {
        return value;
    }

    void setValue(String value) {
        this.value = value;
    }

    public <T> T value(Class<T> clz) {
        return TypeUtils.stringToValue(value(), clz);
    }

    public <T> T defaultValue(Class<T> clz) {
        return TypeUtils.stringToValue(defaultValue, clz);
    }

    public static GlobalConfig valueOf(GlobalConfigVO vo) {
        GlobalConfig conf = new GlobalConfig();
        conf.setName(vo.getName());
        conf.setCategory(vo.getCategory());
        conf.setDefaultValue(vo.getDefaultValue());
        conf.setDescription(vo.getDescription());
        conf.setValue(vo.getValue());
        return conf;
    }

    public static GlobalConfig valueOf(GlobalConfig old) {
        GlobalConfig ng = new GlobalConfig();
        ng.setName(old.getName());
        ng.setValue(old.value());
        ng.setCategory(old.getCategory());
        ng.setDescription(old.getDescription());
        ng.setDefaultValue(old.getDefaultValue());
        ng.setValidatorRegularExpression(old.getValidatorRegularExpression());
        return ng;
    }

    public GlobalConfigVO toVO() {
        GlobalConfigVO vo = new GlobalConfigVO();
        vo.setCategory(category);
        vo.setValue(value);
        vo.setDescription(description);
        vo.setDefaultValue(defaultValue);
        vo.setName(name);
        return vo;
    }

    public static GlobalConfig valueOf(org.zstack.core.config.schema.GlobalConfig.Config c) {
        GlobalConfig conf = new GlobalConfig();
        conf.setName(c.getName());
        conf.setCategory(c.getCategory());
        conf.setDefaultValue(c.getDefaultValue());
        conf.setDescription(c.getDescription());
        conf.setValue(c.getValue());
        conf.setType(c.getType());
        return conf;
    }

    public String getIdentity() {
        return produceIdentity(category, name);
    }

    public static String produceIdentity(String category, String name) {
        return String.format("%s.%s", category, name);
    }

    void validate() {
        validate(value);
    }

    public void validate(String newValue) {
        for  (GlobalConfigValidatorExtensionPoint ext : validators) {
            ext.validateGlobalConfig(category, name, value, newValue);
        }
    }

    void init() {
        evtf.on(s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", category),
                e("name", name)
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                String newValue = Q.New(GlobalConfigVO.class).select(GlobalConfigVO_.value)
                        .eq(GlobalConfigVO_.category, category)
                        .eq(GlobalConfigVO_.name, name)
                        .findValue();
                update(newValue, false);

                UpdateEvent evt = (UpdateEvent)data;
                logger.info(String.format("GlobalConfig[category: %s, name: %s] was updated in other management node[uuid:%s]," +
                        "in line with that change, updated ours. %s --> %s", category, name, nodeUuid, evt.getOldValue(), value));
            }
        });
    }

    private void update(String newValue, boolean localUpdate) {
        // substitute system properties in newValue
        newValue = StringTemplate.substitute(newValue, propertiesMap);

        validate(newValue);

        executeUpdate(newValue, localUpdate);
    }

    private void executeUpdate(String newValue, boolean localUpdate) {
        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, category);
        q.add(GlobalConfigVO_.name, Op.EQ, name);
        GlobalConfigVO vo = q.find();
        final GlobalConfig origin = valueOf(vo);

        for (GlobalConfigBeforeUpdateExtensionPoint ext : beforeUpdateExtensions) {
            try {
                ext.beforeUpdateExtensionPoint(origin, newValue);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                throw t;
            }
        }

        if (localUpdate) {
            for (GlobalConfigBeforeUpdateExtensionPoint ext : localBeforeUpdateExtensions) {
                try {
                    ext.beforeUpdateExtensionPoint(origin, newValue);
                } catch (Throwable t) {
                    logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                    throw t;
                }
            }
        }

        value = newValue;

        if (localUpdate) {
            vo.setValue(newValue);
            dbf.update(vo);

            final GlobalConfig self = this;
            CollectionUtils.safeForEach(localUpdateExtensions, new ForEachFunction<GlobalConfigUpdateExtensionPoint>() {
                @Override
                public void run(GlobalConfigUpdateExtensionPoint ext) {
                    ext.updateGlobalConfig(origin, self);
                }
            });
        }

        for (GlobalConfigUpdateExtensionPoint ext : updateExtensions) {
            try {
                ext.updateGlobalConfig(origin, this);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
            }
        }

        if (localUpdate) {
            UpdateEvent evt = new UpdateEvent();
            evt.setOldValue(origin.value());
            evt.setNewValue(newValue);
            evtf.fire(makeUpdateEventPath(), evt);
        }

        logger.debug(String.format("updated global config[category:%s, name:%s]: %s to %s", category, name, origin.value(), value));
    }

    public void resetValue() {
        updateValue(defaultValue);
    }

    public void updateValue(Object val) {
        if (TypeUtils.nullSafeEquals(value, val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        update(newValue, true);
    }

    public GlobalConfigOptions getOptions() {
        GlobalConfigOptions configOptions = null;
        for  (GlobalConfigQueryExtensionPoint ext : queryExtensions) {
            configOptions = ext.getConfigOptions();

        }
        return configOptions;
    }

    public void updateValueSkipValidation(Object val) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new OperationFailureException(operr("do not allow skip verification"));
        }

        if (TypeUtils.nullSafeEquals(value, val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        // substitute system properties in newValue
        newValue = StringTemplate.substitute(newValue, propertiesMap);
        executeUpdate(newValue, true);
    }

    boolean isLinked() {
        return linked;
    }

    void setLinked(boolean linked) {
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

    void setValidators(List<GlobalConfigValidatorExtensionPoint> validators) {
        this.validators = validators;
    }

    void setUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> updateExtensions) {
        this.updateExtensions = updateExtensions;
    }

    void setLocalUpdateExtensions(List<GlobalConfigUpdateExtensionPoint> localUpdateExtensions) {
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
}
