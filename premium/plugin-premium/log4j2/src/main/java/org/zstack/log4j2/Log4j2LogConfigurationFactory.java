package org.zstack.log4j2;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.Appender;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.appender.AsyncAppender;
import org.apache.logging.log4j.core.config.AppenderRef;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.log.LogConfigurationFactory;
import org.zstack.log.LogConfigurationStruct;
import org.zstack.log.server.LogCategory;
import org.zstack.log.server.LogServerVO;
import org.zstack.log.server.LogServerVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.argerr;
import static org.zstack.core.Platform.operr;

public class Log4j2LogConfigurationFactory implements Component, LogConfigurationFactory {
    private static final CLogger logger = Utils.getLogger(Log4j2LogConfigurationFactory.class);

    @Autowired
    private PluginRegistry pluginRgty;

    public static final String LOG4J2 = "log4j2";

    private Map<String, Log4j2AppenderProxyFactory> log4j2AppenderFactories = new HashMap<>();

    public void createLogConfiguration(LogConfigurationStruct struct, ReturnValueCompletion<String> completion) {
        logger.debug(String.format("create log4j configuration on managementNode[uuid:%s]", Platform.getManagementServerId()));
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        LoggerConfig rootConfig = config.getRootLogger();

        if (rootConfig.getAppenders().keySet().contains(struct.getUuid())) {
            completion.success(null);
            return;
        }

        Log4j2ConfigurationStruct log4j2 = toLog4j2ConfigurationStruct(struct.getConfiguration());

        Log4j2AppenderProxyFactory factory = log4j2AppenderFactories.get(log4j2.getAppenderType());

        if (factory == null) {
            throw new OperationFailureException(operr("No factory found for log4j2 appender type:%s.", log4j2.getAppenderType()));
        }

        String appenderString = factory.createLog4j2Appender(config, log4j2, struct.getUuid());

        AppenderRef ref = AppenderRef.createAppenderRef(struct.getUuid(), struct.getLog4jOfLevel(), null);
        AppenderRef[] refs = new AppenderRef[]{ref};
        Appender appender = AsyncAppender.newBuilder()
                .setConfiguration(config)
                .setName(struct.getUuid())
                .setAppenderRefs(refs)
                .build();
        appender.start();
        rootConfig.addAppender(appender, null, null);
        ctx.updateLoggers();

        completion.success(appenderString);
    }

    @Override
    public void deleteLogConfiguration(String uuid) {
        logger.debug(String.format("delete log4j configuration on managementNode[uuid:%s]", Platform.getManagementServerId()));
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final LoggerConfig rootConfig = config.getRootLogger();
        rootConfig.removeAppender(uuid);
        config.removeLogger(uuid);
        ctx.updateLoggers();
    }

    @Override
    public void loadConfiguration() {
        logger.debug(String.format("load log4j2 configuration on managementNode[uuid:%s]", Platform.getManagementServerId()));
        final LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        final Configuration config = ctx.getConfiguration();
        final LoggerConfig rootConfig = config.getRootLogger();

        List<LogServerVO> logServerVOS = Q.New(LogServerVO.class)
                .eq(LogServerVO_.category, LogCategory.ManagementNodeLog)
                .list();

        for (LogServerVO vo : logServerVOS) {
            Log4j2ConfigurationStruct log4j2s = toLog4j2ConfigurationStruct(vo.getConfiguration());
            if (StringUtils.isEmpty(log4j2s.getAppenderType())) {
                log4j2s.setAppenderType(Log4j2AppenderType.Syslog.name());
            }

            Log4j2AppenderProxyFactory factory = log4j2AppenderFactories.get(log4j2s.getAppenderType());
            if (factory == null) {
                throw new OperationFailureException(operr("No factory found for log4j2 appender type:%s.", log4j2s.getAppenderType()));
            }
            factory.createLog4j2Appender(config, log4j2s, vo.getUuid());

            Level level = vo.getLevel() == null ? Level.ALL : Level.getLevel(vo.getLevel().name());
            AppenderRef ref = AppenderRef.createAppenderRef(vo.getUuid(), level, null);
            AppenderRef[] refs = new AppenderRef[]{ref};
            Appender appender = AsyncAppender.newBuilder()
                    .setConfiguration(config)
                    .setName(vo.getUuid())
                    .setAppenderRefs(refs)
                    .build();
            appender.start();
            rootConfig.addAppender(appender, null, null);
        }

        ctx.updateLoggers();
    }

    @Override
    public String getLogConfigurationType() {
        return LOG4J2;
    }

    @Override
    public void validate(LogConfigurationStruct struct) {
        Log4j2ConfigurationStruct log4j2 = toLog4j2ConfigurationStruct(struct.getConfiguration());

        Log4j2AppenderProxyFactory factory = log4j2AppenderFactories.get(log4j2.getAppenderType());

        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown log4j2 appender type %s", log4j2.getAppenderType()));
        }

        factory.validate(log4j2);
    }

    private Log4j2ConfigurationStruct toLog4j2ConfigurationStruct(String configuration) {
        JsonObject object = new JsonParser().parse(configuration).getAsJsonObject();
        Log4j2ConfigurationStruct struct = new Log4j2ConfigurationStruct();

        String appenderType = getString(object, "type");
        if (StringUtils.isEmpty(appenderType)) {
            appenderType = getString(object, "appenderType");
        }

        struct.setAppenderType(appenderType);
        JsonElement config = object.get("configuration");
        if (config != null && !config.isJsonNull()) {
            struct.setConfiguration(config.isJsonPrimitive() ? config.getAsString() : config.toString());
        }
        return struct;
    }

    private String getString(JsonObject object, String name) {
        JsonElement element = object.get(name);
        return element == null || element.isJsonNull() ? null : element.getAsString();
    }

    private void populateExtensions() {
        for (Log4j2AppenderProxyFactory f : pluginRgty.getExtensionList(Log4j2AppenderProxyFactory.class)) {
            Log4j2AppenderType type = f.getLog4j2AppenderType();

            Log4j2AppenderProxyFactory old = log4j2AppenderFactories.get(type.toString());

            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate Log4j2AppenderFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getLog4j2AppenderType().toString()));
            }

            log4j2AppenderFactories.put(type.toString(), f);
        }
    }

    @Override
    public boolean start() {
        populateExtensions();
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
