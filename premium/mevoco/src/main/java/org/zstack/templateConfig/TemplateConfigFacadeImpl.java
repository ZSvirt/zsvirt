package org.zstack.templateConfig;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.MessageSafe;
import org.zstack.core.config.*;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import org.zstack.core.db.SQL;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.core.asyncbatch.While;
import org.zstack.header.AbstractService;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.message.Message;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.data.StringTemplate;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.path.PathUtil;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class TemplateConfigFacadeImpl extends AbstractService implements TemplateConfigFacade {
    private static final CLogger logger = Utils.getLogger(TemplateConfigFacadeImpl.class);

    @Autowired
    private CloudBus bus;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private GlobalConfigFacade gcf;



    private JAXBContext context;
    private Map<String, TemplateConfig> allConfig = new ConcurrentHashMap<>();
    private static final String TEMPLATE_FOLDER = "template";
    private static final String TEMPLATE_CONFIG_FOLDER = "templateConfig";
    private static final String OTHER_CATEGORY = "Others";

    @Override
    @MessageSafe
    public void handleMessage(Message msg) {
        if (msg instanceof APIUpdateTemplateConfigMsg) {
            handle((APIUpdateTemplateConfigMsg) msg);
        } else if (msg instanceof APIApplyTemplateConfigMsg) {
            handle((APIApplyTemplateConfigMsg) msg);
        } else if (msg instanceof APIResetTemplateConfigMsg) {
            handle((APIResetTemplateConfigMsg) msg);
        } else if (msg instanceof APIRevertTemplateConfigMsg) {
            handle((APIRevertTemplateConfigMsg) msg);
        } else {
            bus.dealWithUnknownMessage(msg);
        }
    }

    private void handle(APIRevertTemplateConfigMsg msg) {
        APIRevertTemplateConfigEvent evt = new APIRevertTemplateConfigEvent(msg.getId());
        Map<String, TemplateConfig> configs = getConfigsByTemplateUuid(msg.getTemplateUuid());

        if (configs.isEmpty()) {
            evt.setError(
                    argerr("unable to find any TemplateConfigs: [templateUuid: %s]", msg.getTemplateUuid()));
            bus.publish(evt);
            return;
        }

        new While<>(configs.values()).step((config, completion) -> {
            try {
                String gcKey = GlobalConfig.produceIdentity(config.getCategory(), config.getName());
                GlobalConfig globalConfig = gcf.getAllConfig().get(gcKey);
                globalConfig.resetValue();
            } catch (GlobalConfigException e) {
                completion.addError(operr(e.getMessage()));
            }
            completion.done();
        }, 10).run(new WhileDoneCompletion(msg) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (!errorCodeList.getCauses().isEmpty()) {
                    evt.setError(errorCodeList.getCauses().get(0));
                }
                bus.publish(evt);
            }
        });
    }

    private void handle(APIResetTemplateConfigMsg msg) {
        APIResetTemplateConfigEvent evt = new APIResetTemplateConfigEvent(msg.getId());
        Map<String, TemplateConfig> configs = getConfigsByTemplateUuid(msg.getTemplateUuid());
        
        if (configs.isEmpty()) {
            evt.setError(
                argerr("Unable to find any TemplateConfigs: [templateUuid: %s]", msg.getTemplateUuid()));
            bus.publish(evt);
            return;
        }
        
        logger.info(String.format("Will reset template [uuid: %s]", msg.getTemplateUuid()));
        for(TemplateConfig templateConfig: configs.values()) {
            templateConfig.updateValue(templateConfig.getDefaultValue());
        }

        logger.info("Successfully reset the template configurations.");
        bus.publish(evt);
    }

    private void handle(APIUpdateTemplateConfigMsg msg) {
        APIUpdateTemplateConfigEvent evt = new APIUpdateTemplateConfigEvent(msg.getId());
        TemplateConfig templateConfig = allConfig.get(msg.getIdentity());
        if (templateConfig == null) {
            ErrorCode err = argerr("Unable to find TemplateConfig[category: %s, name: %s, templateUuid: %s]", msg.getCategory(), msg.getName(), msg.getTemplateUuid());
            evt.setError(err);
            bus.publish(evt);
            return;
        }
        logger.info(String.format("Will update templateConfig: %s, %s to %s", templateConfig.getIdentity(), templateConfig.getValue(), msg.getValue()));
        try {
            templateConfig.updateValue(msg.getValue());
            TemplateConfigInventory inv = TemplateConfigInventory.valueOf(templateConfig.reload());
            evt.setInventory(inv);
        } catch (TemplateConfigException e) {
            evt.setError(argerr(e.getMessage()));
            logger.warn(e.getMessage(), e);
        }
        bus.publish(evt);
    }

    private void handle(APIApplyTemplateConfigMsg msg) {
        APIApplyTemplateConfigEvent evt = new APIApplyTemplateConfigEvent(msg.getId());
        Map<String, TemplateConfig> configs = getConfigsByTemplateUuid(msg.getTemplateUuid());
    
        if (configs.isEmpty()) {
            evt.setError(
                argerr("Unable to find any TemplateConfigs: [templateUuid: %s]", msg.getTemplateUuid()));
            bus.publish(evt);
            return;
        }

        List<String> errorList = new ArrayList<>();
        new While<>(configs.values()).all((c, compl) -> {
            try {
                String gcKey = GlobalConfig.produceIdentity(c.getCategory(), c.getName());
                GlobalConfig globalConfig = gcf.getAllConfig().get(gcKey);
                globalConfig.updateValue(c.getValue());
                GlobalConfigInventory.valueOf(globalConfig.reload());

            } catch (GlobalConfigException e) {
                errorList.add(e.getMessage());
                logger.warn(e.getMessage(), e);
            }
        }).run(new WhileDoneCompletion(evt) {
            @Override
            public void done(ErrorCodeList errorCodeList) {
                if (errorList.size() > 0) {
                    evt.setError(argerr(StringUtils.join(errorList, "\n")));
                } else {
                    evt.setSuccess(true);
                }
            }
        });
        bus.publish(evt);
    }

    private Map<String, TemplateConfig> getConfigsByTemplateUuid(String templateUuid) {
        return allConfig.entrySet().stream()
                .filter(entry -> templateUuid.equals(entry.getValue().getTemplateUuid()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Override
    public String getId() {
        return bus.makeLocalServiceId(TemplateConfigConstant.SERVICE_ID);
    }

    @Override
    public boolean start() {
        TemplateConfigInitializer initializer = new TemplateConfigInitializer();
        initializer.init();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public String updateTemplateConfig(String templateUuid, String category, String name , String value) {
        return null;
    }

    public Map<String, TemplateConfig> getAllConfig() {
        return allConfig;
    }

    @Override
    public Map<String, TemplateConfig> getAllConfig(String templateUuid) {
        return null;
    }

    public <T> T getConfigValue( String templateUuid, String category, String name, Class<T> clz) {
        TemplateConfig c = allConfig.get(TemplateConfig.produceIdentity(templateUuid, category, name ));
        DebugUtils.Assert(c!=null, String.format("cannot find TemplateConfig[category:%s, name:%s]", category, name));
        return c.value(clz);
    }


    @Deprecated
    public TemplateConfig createTemplateConfig(TemplateConfigVO vo) {
        vo = dbf.persistAndRefresh(vo);
        TemplateConfig c = TemplateConfig.valueOf(vo);
        allConfig.put(TemplateConfig.produceIdentity(vo.getTemplateUuid(), vo.getCategory(), vo.getName()), c);
        return c;
    }


    @Deprecated
    public String updateConfig(String templateUuid, String category, String name , String value) {
        TemplateConfig c = allConfig.get(TemplateConfig.produceIdentity(templateUuid, category,name));
        DebugUtils.Assert(c != null, String.format("cannot find TemplateConfig[category:%s, name:%s, templateUuid:%s]", category, name, templateUuid));
        c.updateValue(value);
        return c.getValue();
    }
    class TemplateConfigInitializer {
        Map<String, GlobalConfigTemplate> templatesFromDatabase = new HashMap<String, GlobalConfigTemplate>();
        Map<String, TemplateConfig> configsFromDatabase = new HashMap<String, TemplateConfig>();
        Map<String, String> templateNameUuid = new HashMap<String, String>();
        Map<String, String> propertiesMap = new HashMap<>();

        void init() {
            GLock lock = new GLock(TemplateConfigConstant.LOCK, 320);
            lock.lock();
            try {
                loadTemplate();
                loadConfig();
                initAllConfig();
            } catch (IllegalArgumentException ie) {
                throw ie;
            } catch (Exception e) {
                throw new CloudRuntimeException(e);
            } finally {
                lock.unlock();
            }
        }

        private void initAllConfig() {
            for (TemplateConfig config : allConfig.values()) {
                config.init();
            }
        }

        public void loadTemplate() throws JAXBException{
            loadTemplateFromDatabase();
            loadConfigFromXml(TEMPLATE_FOLDER);
        }

        public void loadConfig() throws JAXBException{
            loadTemplateConfigFromDatabase();
            loadConfigFromXml(TEMPLATE_CONFIG_FOLDER);
        }

        private void validateConfig(String category, String name, String value){
            GlobalConfig config = gcf.getAllConfig().get(String.format("%s.%s", category, name));
            config.validate(value);
        }

        private void loadTemplateFromDatabase() {
            List<GlobalConfigTemplateVO> vos = dbf.listAll(GlobalConfigTemplateVO.class);
            for (GlobalConfigTemplateVO vo : vos) {
                GlobalConfigTemplate t = GlobalConfigTemplate.valueOf(vo);
                templatesFromDatabase.put(t.getIdentity(), t);
            }
        }

        private void loadTemplateConfigFromDatabase() {
            List<TemplateConfigVO> vos = dbf.listAll(TemplateConfigVO.class);
            for (TemplateConfigVO vo : vos) {
                TemplateConfig t = TemplateConfig.valueOf(vo);
                configsFromDatabase.put(t.getIdentity(), t);
            }
        }

        private void loadConfigFromXml(String folderName) throws JAXBException {
            context = JAXBContext.newInstance("org.zstack.templateConfig.schema");
            List<String> filePaths = PathUtil.scanFolderOnClassPath(folderName);
            for (String path : filePaths) {
                File f = new File(path);
                if (folderName.equals(TEMPLATE_FOLDER)){
                    parseTemplate(f);
                }else if(folderName.equals(TEMPLATE_CONFIG_FOLDER)){
                    parseConfig(f);
                }
            }
            //delete form db
            for (TemplateConfig config: configsFromDatabase.values()){
                logger.debug(String.format("Will remove an old template config from database: %s", config.toString()));
                SQL.New(TemplateConfigVO.class)
                        .eq(TemplateConfigVO_.templateUuid, config.getTemplateUuid())
                        .eq(TemplateConfigVO_.category, config.getCategory())
                        .eq(TemplateConfigVO_.name, config.getName())
                        .delete();
            }
        }

        private void parseTemplate(File file) throws JAXBException {
            if (!file.getName().endsWith("xml")) {
                logger.warn(String.format("file[%s] in template folder is not end with .xml, skip it", file.getAbsolutePath()));
                return;
            }
            Unmarshaller unmarshaller = context.createUnmarshaller();
            org.zstack.templateConfig.schema.GlobalConfigTemplate t = (org.zstack.templateConfig.schema.GlobalConfigTemplate) unmarshaller.unmarshal(file);
            for (org.zstack.templateConfig.schema.GlobalConfigTemplate.Config c : t.getConfig()) {
                String name = c.getName();
                name = name == null ? OTHER_CATEGORY : name;
                c.setName(name);
                if (c.getType() == null) {
                    throw new IllegalArgumentException(String.format("GlobalConfigTemplate[name:%s] must have a default type", c.getName()));
                }
                c.setDescription(c.getDescription());
                GlobalConfigTemplate template = GlobalConfigTemplate.valueOf(c);
                GlobalConfigTemplate dbgct = templatesFromDatabase.get(template.getIdentity());
                if (dbgct == null) {
                    logger.debug(String.format("Add a new template config to database: %s", template.getIdentity()));
                    String uuid = Platform.getUuid();
                    template.setUuid(uuid);
                    templateNameUuid.put(name, uuid);
                    dbf.persist(template.toVO());
                }else{
                    templateNameUuid.put(dbgct.getName(), dbgct.getUuid());
                }
            }
        }

        private void parseConfig(File file) throws JAXBException {
            if (!file.getName().endsWith("xml")) {
                logger.warn(String.format("file[%s] in global config folder is not end with .xml, skip it", file.getAbsolutePath()));
                return;
            }

            Unmarshaller unmarshaller = context.createUnmarshaller();
            org.zstack.templateConfig.schema.TemplateConfig tc = (org.zstack.templateConfig.schema.TemplateConfig) unmarshaller.unmarshal(file);
            for (org.zstack.templateConfig.schema.TemplateConfig.Config c : tc.getConfig()) {
                String category = c.getCategory();
                String name = c.getName();
                String gcKey = GlobalConfig.produceIdentity(category, name);
                //the config in templateConfig but not in GlobalConfig
                if (gcf.getAllConfig().get(gcKey) == null){
                    throw new IllegalArgumentException(String.format("TemplateConfig[category:%s, name:%s] not found in GlobalConfig", category, name));
                }

                c.setCategory(category);
                String templateUuid = templateNameUuid.get(c.getTemplateName());
                c.setTemplateUuid(templateUuid);
                // substitute system properties in value and defaultValue

                if (c.getDefaultValue() == null) {
                    throw new IllegalArgumentException(String.format("TemplateConfig[category:%s, name:%s] must have a default value", c.getCategory(), c.getName()));
                } else {
                    c.setDefaultValue(StringTemplate.substitute(c.getDefaultValue(), propertiesMap));
                }
                c.setValue(c.getDefaultValue());
                TemplateConfig config = TemplateConfig.valueOf(c);
                validateConfig(category, name, c.getDefaultValue());
                //update or save to db
                TemplateConfig dbcfg = configsFromDatabase.get(config.getIdentity());
                if (dbcfg != null) {
                    if (!dbcfg.getDefaultValue().equals(config.getDefaultValue())) {
                        logger.debug(String.format("Will update a template config default to database: %s", config.toString()));
                        SQL.New(TemplateConfigVO.class)
                                .eq(TemplateConfigVO_.templateUuid, config.getTemplateUuid())
                                .eq(TemplateConfigVO_.category, config.getCategory())
                                .eq(TemplateConfigVO_.name, config.getName())
                                .set(TemplateConfigVO_.defaultValue, config.getDefaultValue())
                                .update();
                    }
                    config.setValue(dbcfg.getValue());
                    allConfig.put(config.getIdentity(), config);
                    configsFromDatabase.remove(dbcfg.getIdentity());
                }else{
                    logger.debug(String.format("Add a new template config to database: %s", config.toString()));
                    dbf.persist(config.toVO());
                    allConfig.put(config.getIdentity(), config);
                }


            }

        }

    }

}