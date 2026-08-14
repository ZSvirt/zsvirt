package org.zstack.core.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.AbstractService;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.BeanUtils;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.TypeUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.zstack.core.Platform.argerr;

public class GlobalConfigFacadeImpl extends AbstractService implements GlobalConfigFacade {
    private static final CLogger logger = Utils.getLogger(GlobalConfigFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PluginRegistry pluginRgty;

    private JAXBContext context;
    private Map<String, GlobalConfig> allConfig = new ConcurrentHashMap<>();

    private static final String CONFIG_FOLDER = "globalConfig";
    private static final String OTHER_CATEGORY = "Others";

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateGlobalConfigMsg) {
            handle((APIUpdateGlobalConfigMsg) msg);
        } else if (msg instanceof APIGetGlobalConfigOptionsMsg) {
            handle((APIGetGlobalConfigOptionsMsg) msg);
        } else if (msg instanceof APIResetGlobalConfigMsg) {
            handle((APIResetGlobalConfigMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIResetGlobalConfigMsg msg) {
        APIResetGlobalConfigEvent evt = new APIResetGlobalConfigEvent(msg.getId());

        for(GlobalConfig globalConfig: allConfig.values()) {
            try {
                for (GlobalConfigBeforeResetExtensionPoint ext : globalConfig.getBeforeResetExtensions()) {
                    ext.beforeResetExtensionPoint(msg.getSession());
                }
            } catch (SkipResetGlobalConfigException ignored) {
                continue;
            }

            globalConfig.resetValue();
        }

        logger.info("Reset all the system global configurations.");
        bus.publish(evt);
    }

    private void handle(APIUpdateGlobalConfigMsg msg) {
        APIUpdateGlobalConfigEvent evt = new APIUpdateGlobalConfigEvent(msg.getId());
        GlobalConfig globalConfig = allConfig.get(msg.getIdentity());
        if (globalConfig == null) {
            ErrorCode err = argerr("Unable to find GlobalConfig[category: %s, name: %s]", msg.getCategory(), msg.getName());
            evt.setError(err);
            bus.publish(evt);
            return;
        }

        try {
            globalConfig.updateValue(msg.getValue());

            GlobalConfigInventory inv = GlobalConfigInventory.valueOf(globalConfig.reload());
            pluginRgty.getExtensionList(AfterUpdateClobalConfigExtensionPoint.class).forEach(point -> point.saveSaveEncryptAfterUpdateClobalConfig(inv));
            evt.setInventory(inv);
        } catch (GlobalConfigException e) {
            evt.setError(argerr(e.getMessage()));
            logger.warn(e.getMessage(), e);
        }
        
        bus.publish(evt);
    }

    private void handle(APIGetGlobalConfigOptionsMsg msg) {
        APIGetGlobalConfigOptionsReply reply = new APIGetGlobalConfigOptionsReply();
        GlobalConfig globalConfig = allConfig.get(msg.getIdentity());
        if (globalConfig == null) {
            ErrorCode err = argerr("Unable to find GlobalConfig[category: %s, name: %s]", msg.getCategory(), msg.getName());
            reply.setError(err);
            bus.reply(msg, reply);
            return;
        }

        try {
            reply.setOptions(globalConfig.getOptions());
        } catch (GlobalConfigException e) {
            reply.setError(argerr(e.getMessage()));
            logger.warn(e.getMessage(), e);
        }

        bus.reply(msg, reply);
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(GlobalConfigConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        class GlobalConfigInitializer {
            Map<String, GlobalConfig> configsFromXml = new HashMap<String, GlobalConfig>();
            Map<String, GlobalConfig> configsFromDatabase = new HashMap<String, GlobalConfig>();
            List<Field> globalConfigFields = new ArrayList<Field>();
            Map<String, String> propertiesMap = new HashMap<>();

            void init() {
                GLock lock = new GLock(GlobalConfigConstant.LOCK, 320);
                lock.lock();
                try {
                    loadSystemProperties();
                    parseGlobalConfigFields();
                    loadConfigFromXml();
                    loadConfigFromJava();
                    loadConfigFromAutoGeneration();
                    loadConfigFromDatabase();
                    createValidatorForBothXmlAndDatabase();
                    validateConfigFromXml();
                    validateConfigFromDatabase();
                    persistConfigInXmlButNotInDatabase();
                    mergeXmlDatabase();
                    link();
                    initAllConfig(); // don't run before link()
                    allConfig.putAll(configsFromXml);
                    // re-validate after merging xml's with db's
                    validateAll();
                } catch (IllegalArgumentException ie) {
                    throw ie;
                } catch (Exception e) {
                    throw new CloudRuntimeException(e);
                } finally {
                    lock.unlock();
                }
            }

            private void loadSystemProperties() {
                boolean noTrim = System.getProperty("DoNotTrimPropertyFile") != null;
                for (final String name: System.getProperties().stringPropertyNames()) {
                    String value = System.getProperty(name);
                    if (!noTrim) {
                        value = value.trim();
                    }
                    propertiesMap.put(name, value);
                }
            }

            private void parseGlobalConfigFields() {
                Set<Class<?>> definitionClasses = BeanUtils.reflections.getTypesAnnotatedWith(GlobalConfigDefinition.class);
                for (Class def : definitionClasses) {
                    for (Field field : def.getDeclaredFields()) {
                        if (Modifier.isStatic(field.getModifiers()) && GlobalConfig.class.isAssignableFrom(field.getType())) {
                            field.setAccessible(true);

                            try {
                                GlobalConfig config = (GlobalConfig) field.get(null);
                                if (config == null) {
                                    throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                                    "You must assign a value to it using new GlobalConfig(category, name)",
                                            def.getName(), field.getName()));
                                }

                                globalConfigFields.add(field);
                            } catch (IllegalAccessException e) {
                                throw new CloudRuntimeException(e);
                            }
                        }
                    }
                }
            }

            private void loadConfigFromJava() {
                for (Field field : globalConfigFields) {
                    try {
                        GlobalConfig config = (GlobalConfig) field.get(null);
                        if (config == null) {
                            throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                            "You must assign a value to it using new GlobalConfig(category, name)",
                                    field.getDeclaringClass().getName(), field.getName()));
                        }

                        GlobalConfigDef d = field.getAnnotation(GlobalConfigDef.class);
                        if (d == null) {
                            continue;
                        }
                        // substitute system properties in defaultValue
                        String defaultValue = StringTemplate.substitute(d.defaultValue(), propertiesMap);

                        GlobalConfig c = new GlobalConfig();
                        c.setCategory(config.getCategory());
                        c.setName(config.getName());
                        c.setDescription(d.description());
                        c.setDefaultValue(defaultValue);
                        c.setValue(defaultValue);
                        c.setType(d.type().getName());
                        if (!"".equals(d.validatorRegularExpression())) {
                            c.setValidatorRegularExpression(d.validatorRegularExpression());
                        }

                        if (configsFromXml.containsKey(c.getIdentity())) {
                            throw new CloudRuntimeException(String.format("duplicate global configuration. %s defines a" +
                                    " global config[category: %s, name: %s] that has been defined by a XML configure or" +
                                    " another java class", field.getDeclaringClass().getName(), c.getCategory(), c.getName()));
                        }

                        configsFromXml.put(c.getIdentity(), c);
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }
            }

            private void initAllConfig() {
                for (GlobalConfig config : configsFromXml.values()) {
                    config.init();
                }
            }

            private void mergeXmlDatabase() {
                for (GlobalConfig g : configsFromDatabase.values()) {
                    GlobalConfig x = configsFromXml.get(g.getIdentity());
                    if (x == null) {
                        configsFromXml.put(g.getIdentity(), g);
                    } else {
                        x.setValue(g.value());
                        x.setDefaultValue(g.getDefaultValue());
                    }
                }
            }

            private void validateAll() {
                for (GlobalConfig g : allConfig.values()) {
                    g.normalize();

                    try {
                        g.validate();
                    } catch (Exception e) {
                        throw new IllegalArgumentException(String.format("exception happened when validating global config:\n%s", g.toString()), e);
                    }
                }
            }

            private void validateConfigFromDatabase() {
                logger.debug("validating global config loaded from database");
                for (GlobalConfig g : configsFromDatabase.values()) {
                    g.validate();
                }
            }

            private void validateConfigFromXml() {
                logger.debug("validating global config loaded from XML files");
                for (GlobalConfig g : configsFromXml.values()) {
                    g.validate();
                }
            }

            private void persistConfigInXmlButNotInDatabase() {
                List<GlobalConfigVO> toSave = new ArrayList<GlobalConfigVO>();  // new config options
                List<GlobalConfig> toRemove = new ArrayList<>(); // obsolete config options
                List<GlobalConfig> toUpdate = new ArrayList<>(); // update both defaultValue and value
                List<GlobalConfig> toUpdate2 = new ArrayList<>(); // only update defaultValue
                List<GlobalConfig> toUpdate3 = new ArrayList<>(); // configs' value is not match type (normally the values were from an old zstack version)

                for (GlobalConfig config : configsFromXml.values()) {
                    GlobalConfig dbcfg = configsFromDatabase.get(config.getIdentity());
                    if (dbcfg != null) {
                        if (!dbcfg.getDefaultValue().equals(config.getDefaultValue())) {
                            logger.debug(String.format("Will update a global config to database: %s", config.toString()));

                            if (dbcfg.getDefaultValue().equals(dbcfg.value())) {
                                toUpdate.add(config);
                            } else {
                                toUpdate2.add(config);
                            }
                        }

                        continue;
                    }

                    logger.debug(String.format("Add a new global config to database: %s", config.toString()));
                    toSave.add(config.toVO());
                }

                for (GlobalConfig config : configsFromDatabase.values()) {
                    if (!configsFromXml.containsKey(config.getIdentity())) {
                        toRemove.add(config);
                    } else {
                        config.setType(configsFromXml.get(config.getIdentity()).getType());
                        String oldValue = config.value();
                        config.normalize();
                        if (!oldValue.equals(config.value())) {
                            logger.warn(String.format("[%s] found value: [%s] not matched the type: [%s], update to: [%s]",
                                    config.getIdentity(), oldValue, config.getType(), config.value()));
                            toUpdate3.add(config);
                        }
                    }
                }

                if (!toSave.isEmpty()) {
                    dbf.persistCollection(toSave);
                }

                for (GlobalConfig config : toRemove) {
                    logger.debug(String.format("Will remove an old global config from database: %s", config.toString()));
                    SQL.New(GlobalConfigVO.class)
                            .eq(GlobalConfigVO_.category, config.getCategory())
                            .eq(GlobalConfigVO_.name, config.getName())
                            .delete();
                }

                for (GlobalConfig config : toUpdate) {
                    SQL.New(GlobalConfigVO.class)
                            .eq(GlobalConfigVO_.category, config.getCategory())
                            .eq(GlobalConfigVO_.name, config.getName())
                            .set(GlobalConfigVO_.defaultValue, config.getDefaultValue())
                            .set(GlobalConfigVO_.value, config.value())
                            .update();
                    configsFromDatabase.get(config.getIdentity()).setValue(config.value());
                }

                for (GlobalConfig config : toUpdate2) {
                    SQL.New(GlobalConfigVO.class)
                            .eq(GlobalConfigVO_.category, config.getCategory())
                            .eq(GlobalConfigVO_.name, config.getName())
                            .set(GlobalConfigVO_.defaultValue, config.getDefaultValue())
                            .update();
                }

                for (GlobalConfig config : toUpdate3) {
                    SQL.New(GlobalConfigVO.class)
                            .eq(GlobalConfigVO_.category, config.getCategory())
                            .eq(GlobalConfigVO_.name, config.getName())
                            .set(GlobalConfigVO_.value, config.value())
                            .update();
                }
            }

            private void loadConfigFromAutoGeneration() {
                for (GlobalConfigInitExtensionPoint ext : pluginRgty.getExtensionList(GlobalConfigInitExtensionPoint.class)) {
                    List<GlobalConfig> tmp = ext.getGenerationGlobalConfig();
                    Optional.ofNullable(tmp).ifPresent(configs -> configs.forEach(it ->
                            configsFromXml.put(it.getIdentity(), it)
                    ));
                }
            }

            private void loadConfigFromDatabase() {
                List<GlobalConfigVO> vos = dbf.listAll(GlobalConfigVO.class);
                for (GlobalConfigVO vo : vos) {
                    GlobalConfig c = GlobalConfig.valueOf(vo);
                    configsFromDatabase.put(c.getIdentity(), c);
                }
            }

            private void loadConfigFromXml() throws JAXBException {
                context = JAXBContext.newInstance("org.zstack.core.config.schema");
                List<String> filePaths = PathUtil.scanFolderOnClassPath(CONFIG_FOLDER);
                for (String path : filePaths) {
                    File f = new File(path);
                    parseConfig(f);
                }
            }

            private void createValidator(final GlobalConfig g) throws ClassNotFoundException {
                g.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                    Class<?> typeClass;
                    Method typeClassValueOfMethod;
                    String regularExpression;

                    {
                        if (String.class.getName().equals(g.getType())) {
                            typeClass = String.class;

                            try {
                                typeClassValueOfMethod = String.class.getMethod("valueOf", Object.class);
                            } catch (Exception ignored) {}
                        } else if (g.getType() != null) {
                            typeClass = Class.forName(g.getType());

                            try {
                                typeClassValueOfMethod = typeClass.getMethod("valueOf", String.class);
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] specifies type[%s] which doesn't have a static valueOf() method, ignore this type",
                                        g.getCategory(), g.getName(), g.getType());
                                logger.warn(err);
                            }
                        }

                        regularExpression = g.getValidatorRegularExpression();
                    }

                    @Override
                    public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                        if (typeClassValueOfMethod != null) {
                            try {
                                typeClassValueOfMethod.invoke(typeClass, newValue);
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] is of type %s, the value[%s] cannot be converted to that type, %s",
                                        g.getCategory(), g.getName(), typeClass.getName(), newValue, e.getMessage());
                                throw new GlobalConfigException(err, e);
                            }

                            try {
                                typeClassValueOfMethod.invoke(typeClass, g.getDefaultValue());
                            } catch (Exception e) {
                                String err = String.format("GlobalConfig[category:%s, name:%s] is of type %s, the default value[%s] cannot be converted to that type, %s",
                                        g.getCategory(), g.getName(), typeClass.getName(), g.getDefaultValue(), e.getMessage());
                                throw new GlobalConfigException(err, e);
                            }
                        }

                        if (typeClass != null && (Boolean.class).isAssignableFrom(typeClass)) {
                            if (newValue == null ||
                                    (!newValue.equalsIgnoreCase("true") &&
                                            !newValue.equalsIgnoreCase("false"))
                                    ) {
                                String err = String.format("GlobalConfig[category:%s, name:%s]'s value[%s] is not a valid boolean string[true, false].",
                                        g.getCategory(), g.getName(), newValue);
                                throw new GlobalConfigException(err);
                            }

                            if (g.getDefaultValue() == null ||
                                    (!g.getDefaultValue().equalsIgnoreCase("true") &&
                                            !g.getDefaultValue().equalsIgnoreCase("false"))
                                    ) {
                                String err = String.format("GlobalConfig[category:%s, name:%s]'s default value[%s] is not a valid boolean string[true, false].",
                                        g.getCategory(), g.getName(), g.getDefaultValue());
                                throw new GlobalConfigException(err);

                            }
                        }

                        if (regularExpression != null) {
                            Pattern p = Pattern.compile(regularExpression);
                            if (newValue != null) {
                                Matcher mt = p.matcher(newValue);
                                if (!mt.matches()) {
                                    String err = String.format("GlobalConfig[category:%s, name:%s]'s value[%s] doesn't match validatorRegularExpression[%s]",
                                            g.getCategory(), g.getName(), newValue, regularExpression);
                                    throw new GlobalConfigException(err);
                                }
                            }
                            if (g.getDefaultValue() != null) {
                                Matcher mt = p.matcher(g.getDefaultValue());
                                if (!mt.matches()) {
                                    String err = String.format("GlobalConfig[category:%s, name:%s]'s default value[%s] doesn't match validatorRegularExpression[%s]",
                                            g.getCategory(), g.getName(), g.getDefaultValue(), regularExpression);
                                    throw new GlobalConfigException(err);
                                }
                            }
                        }
                    }
                });
            }

            private void createValidatorForBothXmlAndDatabase() throws ClassNotFoundException {
                for (GlobalConfig g : configsFromXml.values()) {
                    createValidator(g);
                }
                for (GlobalConfig g : configsFromDatabase.values()) {
                    createValidator(g);
                }
            }

            private void parseConfig(File file) throws JAXBException {
                if (!file.getName().endsWith("xml")) {
                    logger.warn(String.format("file[%s] in global config folder is not end with .xml, skip it", file.getAbsolutePath()));
                    return;
                }

                Unmarshaller unmarshaller = context.createUnmarshaller();
                org.zstack.core.config.schema.GlobalConfig gb = (org.zstack.core.config.schema.GlobalConfig) unmarshaller.unmarshal(file);
                for (org.zstack.core.config.schema.GlobalConfig.Config c : gb.getConfig()) {
                    String category = c.getCategory();
                    category = category == null ? OTHER_CATEGORY : category;
                    c.setCategory(category);
                    // substitute system properties in value and defaultValue
                    if (c.getDefaultValue() == null) {
                        throw new IllegalArgumentException(String.format("GlobalConfig[category:%s, name:%s] must have a default value", c.getCategory(), c.getName()));
                    } else {
                        c.setDefaultValue(StringTemplate.substitute(c.getDefaultValue(), propertiesMap));
                    }
                    if (c.getValue() == null) {
                        c.setValue(c.getDefaultValue());
                    } else {
                        c.setValue(StringTemplate.substitute(c.getValue(), propertiesMap));
                    }
                    GlobalConfig config = GlobalConfig.valueOf(c);
                    if (configsFromXml.containsKey(config.getIdentity())) {
                        throw new IllegalArgumentException(String.format("duplicate GlobalConfig[category: %s, name: %s]", config.getCategory(), config.getName()));
                    }
                    configsFromXml.put(config.getIdentity(), config);
                }
            }

            private void link() {
                for (Field field : globalConfigFields) {
                    field.setAccessible(true);
                    try {
                        GlobalConfig config = (GlobalConfig) field.get(null);
                        if (config == null) {
                            throw new CloudRuntimeException(String.format("GlobalConfigDefinition[%s] defines a null GlobalConfig[%s]." +
                                    "You must assign a value to it using new GlobalConfig(category, name)",
                                    field.getDeclaringClass().getName(), field.getName()));
                        }

                        link(field, config);
                    } catch (IllegalAccessException e) {
                        throw new CloudRuntimeException(e);
                    }
                }

                for (GlobalConfig c : configsFromXml.values()) {
                    if (!c.isLinked()) {
                        logger.warn(String.format("GlobalConfig[category: %s, name: %s] is not linked to any definition", c.getCategory(), c.getName()));
                    }
                }
            }

            private void link(Field field, final GlobalConfig old) throws IllegalAccessException {
                GlobalConfig xmlConfig = configsFromXml.get(old.getIdentity());
                DebugUtils.Assert(xmlConfig != null, String.format("unable to find GlobalConfig[category:%s, name:%s] for linking to %s.%s",
                        old.getCategory(), old.getName(), field.getDeclaringClass().getName(), field.getName()));
                final GlobalConfig config;
                if (old.getEvtf() == null) {
                    if (xmlConfig.getEvtf() != null) {
                        config = xmlConfig;
                    } else {
                        config = new GlobalConfig();
                        config.copy(xmlConfig);
                    }
                    logger.debug(String.format("GlobalConfig[category: %s, name: %s] static field was initialized before Spring autowiring, relink to autowired instance",
                            config.getCategory(), config.getName()));
                } else {
                    config = old.copy(xmlConfig);
                }
                field.set(null, config);
                // all global config base on Field allConfig which is origin from configsFromXml, so update its value
                configsFromXml.put(old.getIdentity(), config);

                final GlobalConfigValidation at = field.getAnnotation(GlobalConfigValidation.class);
                if (at != null) {
                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                            if (at.notNull() && value == null) {
                                throw new GlobalConfigException(String.format("%s cannot be null", config.getCanonicalName()));
                            }
                        }
                    });

                    config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                        @Override
                        public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                            if (at.notEmpty() && newValue.trim().equals("")) {
                                throw new GlobalConfigException(String.format("%s cannot be empty string", config.getCanonicalName()));
                            }
                        }
                    });

                    if (at.inNumberRange().length > 0 || at.min() != Long.MIN_VALUE || at.max() != Long.MAX_VALUE) {
                        if (config.getType() != null && TypeUtils.isTypeOf(config.getType(), Long.class, Integer.class)) {
                            throw new CloudRuntimeException(String.format("%s has @GlobalConfigValidation defined on field[%s.%s] which indicates its numeric type, but its type is neither Long nor Integer, it's %s",
                                    config.getCanonicalName(), field.getDeclaringClass(), field.getName(), config.getType()));
                        }
                        if (config.getType() == null) {
                            logger.warn(String.format("%s has @GlobalConfigValidation defined on field[%s.%s] which indicates it's numeric type, but its is null, assume it's Long type",
                                    config.getCanonicalName(), field.getDeclaringClass(), field.getName()));
                            config.setType(Long.class.getName());
                        }
                    }

                    if (at.max() != Long.MAX_VALUE) {
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.parseLong(value);
                                    if (num > at.max()) {
                                        throw new GlobalConfigException(String.format("%s should not greater than %s, but got %s",
                                                config.getCanonicalName(), at.max(), num));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.min() != Long.MIN_VALUE) {
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.parseLong(value);
                                    if (num < at.min()) {
                                        throw new GlobalConfigException(String.format("%s should not less than %s, but got %s",
                                                config.getCanonicalName(), at.min(), num));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.inNumberRange().length > 0) {
                        DebugUtils.Assert(at.inNumberRange().length == 2, String.format("@GlobalConfigValidation.inNumberRange defined on field[%s.%s] must have two elements, where the first one is lower bound and the second one is upper bound",
                                field.getDeclaringClass(), field.getName()));

                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String value) throws GlobalConfigException {
                                try {
                                    long num = Long.parseLong(value);
                                    long lowBound = at.inNumberRange()[0];
                                    long upBound = at.inNumberRange()[1];
                                    if (!(num >= lowBound && num <= upBound)) {
                                        throw new GlobalConfigException(String.format("%s must in range of [%s, %s]",
                                                config.getCanonicalName(), lowBound, upBound));
                                    }
                                } catch (NumberFormatException e) {
                                    throw new GlobalConfigException(String.format("%s is not a number or out of range of a Long type", value), e);
                                }
                            }
                        });
                    }

                    if (at.validValues().length > 0) {
                        final List<String> validValues = new ArrayList<String>();
                        Collections.addAll(validValues, at.validValues());
                        config.installValidateExtension(new GlobalConfigValidatorExtensionPoint() {
                            @Override
                            public void validateGlobalConfig(String category, String name, String oldValue, String newValue) throws GlobalConfigException {
                                if (!validValues.contains(newValue)) {
                                    throw new GlobalConfigException(String.format("%s is not a valid value. Valid values are %s", newValue, validValues));
                                }
                            }
                        });
                    }
                }

                boolean isInteger = Integer.class.getName().equals(config.getType());
                config.installQueryExtension(new GlobalConfigQueryExtensionPoint() {
                    @Override
                    public GlobalConfigOptions getConfigOptions() {
                        GlobalConfigOptions options = new GlobalConfigOptions();

                        if (at.validValues().length > 0) {
                            options.setValidValue(Arrays.asList(at.validValues()));
                        } else if (at.inNumberRange().length == 2){
                            options.setNumberLessThanOrEqual(at.inNumberRange()[1]);
                            options.setNumberGreaterThanOrEqual(at.inNumberRange()[0]);
                        } else if (at.max() != Long.MAX_VALUE || at.min() != Long.MIN_VALUE) {
                            if (isInteger) {
                                options.setNumberLessThanOrEqual((long) Integer.MAX_VALUE);
                                options.setNumberGreaterThanOrEqual((long) Integer.MIN_VALUE);
                            } else {
                                options.setNumberLessThanOrEqual(Long.MAX_VALUE);
                                options.setNumberGreaterThanOrEqual(Long.MIN_VALUE);
                            }
                        }

                        if (at.max() != Long.MAX_VALUE) {
                            options.setNumberLessThanOrEqual(at.max());
                        }

                        if (at.min() != Long.MIN_VALUE) {
                            options.setNumberGreaterThanOrEqual(at.min());
                        }

                        return options;
                    }
                });

                config.setConfigDef(field.getAnnotation(GlobalConfigDef.class));
                config.setLinked(true);
                if (logger.isTraceEnabled()) {
                    logger.trace(String.format("linked GlobalConfig[category:%s, name:%s, value:%s] to %s.%s",
                            config.getCategory(), config.getName(), config.getDefaultValue(), field.getDeclaringClass().getName(), field.getName()));
                }
            }
        }

        GlobalConfigInitializer initializer = new GlobalConfigInitializer();
        initializer.init();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public Map<String, GlobalConfig> getAllConfig() {
        return allConfig;
    }

    @Override
    public <T> T getConfigValue(String category, String name, Class<T> clz) {
        GlobalConfig c = allConfig.get(GlobalConfig.produceIdentity(category, name));
        DebugUtils.Assert(c!=null, String.format("cannot find GlobalConfig[category:%s, name:%s]", category, name));
        return c.value(clz);
    }

    /**
     * do not use it until support multi management node
     *
     * @param vo
     * @return
     */
    @Override
    @Deprecated
    public GlobalConfig createGlobalConfig(GlobalConfigVO vo) {
        vo = dbf.persistAndRefresh(vo);
        GlobalConfig c = GlobalConfig.valueOf(vo);
        allConfig.put(GlobalConfig.produceIdentity(vo.getCategory(), vo.getName()), c);
        return c;
    }

    /**
     * do not use it until support multi management node
     *
     * @param category
     * @param name
     * @param value
     * @return
     */
    @Override
    @Deprecated
    public String updateConfig(String category, String name, String value) {
        GlobalConfig c = allConfig.get(GlobalConfig.produceIdentity(category,name));
        DebugUtils.Assert(c != null, String.format("cannot find GlobalConfig[category:%s, name:%s]", category, name));
        c.updateValue(value);
        return c.value();
    }
}
