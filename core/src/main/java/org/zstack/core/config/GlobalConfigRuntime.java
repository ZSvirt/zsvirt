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
import org.zstack.mevoco.PremiumGlobalConfig;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.operr;
import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.StringDSL.s;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class GlobalConfigRuntime extends PremiumGlobalConfig {
    private static final CLogger logger = Utils.getLogger(GlobalConfigRuntime.class);

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

    public GlobalConfigRuntime(String category, String name) {
        super(category, name);
    }

    GlobalConfigRuntime() {
        super(null, null);
    }

    @Override
    public GlobalConfigRuntime asRuntime() {
        return this;
    }

    @Override
    public boolean isSpringReady() {
        return evtf != null;
    }

    private String makeUpdateEventPath() {
        return s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("nodeUuid", Platform.getManagementServerId()),
                e("category", getCategory()),
                e("name", getName())
        ));
    }

    public GlobalConfigVO reload() {
        // Note: SimpleQuery can not be replaced by Q: deadlock found when ENABLE_PASSWORD_ENCRYPT update
        // and DataCryptoCase will fail
        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, getCategory());
        q.add(GlobalConfigVO_.name, Op.EQ, getName());
        return q.find();
    }

    public static GlobalConfigRuntime valueOf(GlobalConfigVO vo) {
        GlobalConfigRuntime conf = new GlobalConfigRuntime();
        conf.setName(vo.getName());
        conf.setCategory(vo.getCategory());
        conf.setDefaultValue(vo.getDefaultValue());
        conf.setDescription(vo.getDescription());
        conf.setValue(vo.getValue());
        return conf;
    }

    public static GlobalConfigRuntime valueOf(GlobalConfig old) {
        GlobalConfigRuntime ng = new GlobalConfigRuntime();
        ng.copy(old);
        return ng;
    }

    public GlobalConfigVO toVO() {
        GlobalConfigVO vo = new GlobalConfigVO();
        vo.setCategory(getCategory());
        vo.setValue(value());
        vo.setDescription(getDescription());
        vo.setDefaultValue(getDefaultValue());
        vo.setName(getName());
        return vo;
    }

    public static GlobalConfigRuntime valueOf(org.zstack.core.config.schema.GlobalConfig.Config c) {
        GlobalConfigRuntime conf = new GlobalConfigRuntime();
        conf.setName(c.getName());
        conf.setCategory(c.getCategory());
        conf.setDefaultValue(c.getDefaultValue());
        conf.setDescription(c.getDescription());
        conf.setValue(c.getValue());
        conf.setType(c.getType());
        return conf;
    }

    public static GlobalConfigRuntime valueOf(GlobalConfigDescription d) {
        GlobalConfigRuntime conf = new GlobalConfigRuntime();
        conf.setName(d.name);
        conf.setCategory(d.category);
        conf.setDefaultValue(d.defaultValue);
        conf.setDescription(d.description);
        conf.setValue(d.defaultValue);
        conf.setType(d.type);
        conf.setValidatorRegularExpression(d.regularExpression);
        return conf;
    }

    void init() {
        evtf.on(s(GlobalConfigCanonicalEvents.UPDATE_EVENT_PATH).formatByMap(map(
                e("category", getCategory()),
                e("name", getName())
        )), new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                String nodeUuid = (String) tokens.get("nodeUuid");
                if (Platform.getManagementServerId().equals(nodeUuid)) {
                    return;
                }

                String newValue = Q.New(GlobalConfigVO.class).select(GlobalConfigVO_.value)
                        .eq(GlobalConfigVO_.category, getCategory())
                        .eq(GlobalConfigVO_.name, getName())
                        .findValue();
                update(newValue, false);

                UpdateEvent evt = (UpdateEvent) data;
                logger.info(String.format("GlobalConfig[category: %s, name: %s] was updated in other management node[uuid:%s]," +
                        "in line with that change, updated ours. %s --> %s", getCategory(), getName(), nodeUuid, evt.getOldValue(), value()));
            }
        });
    }

    private void update(String newValue, boolean localUpdate) {
        newValue = StringTemplate.substitute(newValue, propertiesMap);
        validate(newValue);
        executeUpdate(newValue, localUpdate);
    }

    private void executeUpdate(String newValue, boolean localUpdate) {
        SimpleQuery<GlobalConfigVO> q = dbf.createQuery(GlobalConfigVO.class);
        q.add(GlobalConfigVO_.category, Op.EQ, getCategory());
        q.add(GlobalConfigVO_.name, Op.EQ, getName());
        GlobalConfigVO vo = q.find();
        final GlobalConfig origin = valueOf(vo);

        for (GlobalConfigBeforeUpdateExtensionPoint ext : getBeforeUpdateExtensions()) {
            try {
                ext.beforeUpdateExtensionPoint(origin, newValue);
            } catch (Throwable t) {
                logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                throw t;
            }
        }

        if (localUpdate) {
            for (GlobalConfigBeforeUpdateExtensionPoint ext : getLocalBeforeUpdateExtensions()) {
                try {
                    ext.beforeUpdateExtensionPoint(origin, newValue);
                } catch (Throwable t) {
                    logger.warn(String.format("unhandled exception when calling %s", ext.getClass()), t);
                    throw t;
                }
            }
        }

        setValue(newValue);

        if (localUpdate) {
            vo.setValue(newValue);
            dbf.update(vo);

            final GlobalConfig self = this;
            CollectionUtils.safeForEach(getLocalUpdateExtensions(), ext -> ext.updateGlobalConfig(origin, self));
        }

        for (GlobalConfigUpdateExtensionPoint ext : getUpdateExtensions()) {
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

        logger.debug(String.format("updated global config[category:%s, name:%s]: %s to %s",
                getCategory(), getName(), origin.value(), value()));
    }

    @Override
    public void updateValue(Object val) {
        if (TypeUtils.nullSafeEquals(value(), val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        update(newValue, true);
    }

    @Override
    public void updateValueSkipValidation(Object val) {
        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            throw new OperationFailureException(operr("do not allow skip verification"));
        }

        if (TypeUtils.nullSafeEquals(value(), val)) {
            return;
        }

        String newValue = val == null ? null : val.toString();
        newValue = StringTemplate.substitute(newValue, propertiesMap);
        executeUpdate(newValue, true);
    }
}
