package org.zstack.templateConfig;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.templateConfig.TemplateConfigCanonicalEvents.TemplateConfigUpdateEvent;
import org.zstack.utils.CollectionUtils;
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

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TemplateConfig {
    private static final CLogger logger = Utils.getLogger(TemplateConfig.class);

    private String name;
    private String category;
    private String templateUuid;
    private String type;
    private String validatorRegularExpression;
    private String defaultValue;
    private volatile String value;
    private transient List<TemplateConfigUpdateExtensionPoint> updateExtensions = new ArrayList<>();
    private transient List<TemplateConfigBeforeUpdateExtensionPoint> beforeUpdateExtensions = new ArrayList<>();
    private transient List<TemplateConfigUpdateExtensionPoint> localUpdateExtensions = new ArrayList<>();
    private transient List<TemplateConfigBeforeUpdateExtensionPoint> localBeforeUpdateExtensions = new ArrayList<>();

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private GlobalConfigFacade gcf;

    public EventFacade getEvtf() {
        return evtf;
    }

    @Override
    public String toString() {
        return JSONObjectUtil.toJsonString(map(
                e("name", name),
                e("category", category),
                e("type", type),
                e("templateUuid", templateUuid),
                e("defaultValue", defaultValue),
                e("value", value),
                e("validatorRegularExpression", validatorRegularExpression)
        ));
    }

    public TemplateConfig(String category, String name, String templateUuid) {
        this.category = category;
        this.name = name;
        this.templateUuid = templateUuid;
    }

    public TemplateConfig() {
    }


    public TemplateConfigVO reload() {
        SimpleQuery<TemplateConfigVO> q = dbf.createQuery(TemplateConfigVO.class);
        q.add(TemplateConfigVO_.category, Op.EQ, category);
        q.add(TemplateConfigVO_.name, Op.EQ, name);
        q.add(TemplateConfigVO_.templateUuid, Op.EQ, templateUuid);
        return q.find();
    }

    public void installUpdateExtension(TemplateConfigUpdateExtensionPoint ext) {
        updateExtensions.add(ext);
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

    public String getValue() {
        return value;
    }

    void setValue(String value) { this.value = value; }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }


    public <T> T value(Class<T> clz) {
        return TypeUtils.stringToValue(getValue(), clz);
    }

    public <T> T defaultValue(Class<T> clz) {
        return TypeUtils.stringToValue(defaultValue, clz);
    }

    public static TemplateConfig valueOf(TemplateConfigVO vo) {
        TemplateConfig conf = new TemplateConfig();
        conf.setName(vo.getName());
        conf.setCategory(vo.getCategory());
        conf.setTemplateUuid(vo.getTemplateUuid());
        conf.setDefaultValue(vo.getDefaultValue());
        conf.setValue(vo.getValue());
        return conf;
    }

    public static TemplateConfig valueOf(TemplateConfig old) {
        TemplateConfig nt = new TemplateConfig();
        nt.setName(old.getName());
        nt.setValue(old.getValue());
        nt.setCategory(old.getCategory());
        nt.setDefaultValue(old.getDefaultValue());
        nt.setValidatorRegularExpression(old.getValidatorRegularExpression());
        return nt;
    }

    public TemplateConfigVO toVO() {
        TemplateConfigVO vo = new TemplateConfigVO();
        vo.setCategory(category);
        vo.setValue(value);
        vo.setDefaultValue(defaultValue);
        vo.setName(name);
        vo.setTemplateUuid(templateUuid);
        return vo;
    }

    public static TemplateConfig valueOf(org.zstack.templateConfig.schema.TemplateConfig.Config c) {
        TemplateConfig conf = new TemplateConfig();
        conf.setName(c.getName());
        conf.setCategory(c.getCategory());
        conf.setTemplateUuid(c.getTemplateUuid());
        conf.setDefaultValue(c.getDefaultValue());
        conf.setValue(c.getValue());
        conf.setType(c.getType());
        return conf;
    }

    public String getIdentity() {
        return produceIdentity(templateUuid, category, name);
    }

    public static String produceIdentity(String templateUuid, String category, String name) {
        return String.format("%s.%s.%s", templateUuid, category, name);
    }


    public void validate(String newValue) {
        GlobalConfig config = gcf.getAllConfig().get(String.format("%s.%s", category, name));
        config.validate(newValue);
    }

    void init() {
        evtf.on(s(TemplateConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", category),
                e("name", name),
                e("templateUuid", templateUuid)
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                String newValue = Q.New(TemplateConfigVO.class).select(TemplateConfigVO_.value)
                        .eq(TemplateConfigVO_.category, category)
                        .eq(TemplateConfigVO_.name, name)
                        .eq(TemplateConfigVO_.templateUuid, templateUuid)
                        .findValue();
                update(newValue, false);

                TemplateConfigUpdateEvent evt = (TemplateConfigUpdateEvent) data;
                logger.info(String.format("templateConfig[category: %s, name: %s, templateUuid: %s] was updated in other management node[uuid:%s]," +
                        "in line with that change, updated ours. %s --> %s", category, name, templateUuid, nodeUuid, evt.getOldValue(), value));
            }
        });
    }

    private void update(String newValue, boolean localUpdate) {
        validate(newValue);
        logger.info(String.format("validate successfully, will update [templateUuid: %s, category:%s, name:%s] to %s.",templateUuid, category, name, newValue));
        SimpleQuery<TemplateConfigVO> q = dbf.createQuery(TemplateConfigVO.class);
        q.add(TemplateConfigVO_.category, Op.EQ, category);
        q.add(TemplateConfigVO_.name, Op.EQ, name);
        q.add(TemplateConfigVO_.templateUuid, Op.EQ, templateUuid);
        TemplateConfigVO vo = q.find();
        final TemplateConfig origin = valueOf(vo);

        for (TemplateConfigBeforeUpdateExtensionPoint ext : beforeUpdateExtensions) {
            try {
                ext.beforeUpdateExtensionPoint(origin, newValue);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                throw new TemplateConfigException(t);
            }
        }

        if (localUpdate) {
            for (TemplateConfigBeforeUpdateExtensionPoint ext : localBeforeUpdateExtensions) {
                try {
                    ext.beforeUpdateExtensionPoint(origin, newValue);
                } catch (Throwable t) {
                    logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                    throw new TemplateConfigException(t);
                }
            }
        }

        value = newValue;

        if (localUpdate) {
            vo.setValue(newValue);
            dbf.update(vo);

            final TemplateConfig self = this;
            CollectionUtils.safeForEach(localUpdateExtensions, new ForEachFunction<TemplateConfigUpdateExtensionPoint>() {
                @Override
                public void run(TemplateConfigUpdateExtensionPoint ext) {
                    ext.updateTemplateConfig(origin, self);
                }
            });
            TemplateConfigUpdateEvent evt = new TemplateConfigUpdateEvent();
            evt.setOldValue(origin.getValue());
            evt.setNewValue(newValue);
            evtf.fire(makeUpdateEventPath(), evt);
        }

        for (TemplateConfigUpdateExtensionPoint ext : updateExtensions) {
            try {
                ext.updateTemplateConfig(origin, this);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                throw new TemplateConfigException(t);
            }
        }


        logger.debug(String.format("Successfully updated template config[templateUuid: %s, category:%s, name:%s]: %s to %s",templateUuid, category, name, origin.getValue(), value));
    }

    private String makeUpdateEventPath() {
        return s(TemplateConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("templateUuid", templateUuid),
                e("category", category),
                e("name", name)
        ));
    }

    public void updateValue(Object val) {
        if (TypeUtils.nullSafeEquals(value, val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        update(newValue, true);
    }

    public List<TemplateConfigUpdateExtensionPoint> getUpdateExtensions() {
        return updateExtensions;
    }

    public List<TemplateConfigBeforeUpdateExtensionPoint> getBeforeUpdateExtensions() {
        return beforeUpdateExtensions;
    }

    public List<TemplateConfigUpdateExtensionPoint> getLocalUpdateExtensions() {
        return localUpdateExtensions;
    }

    public List<TemplateConfigBeforeUpdateExtensionPoint> getLocalBeforeUpdateExtensions() {
        return localBeforeUpdateExtensions;
    }
}
