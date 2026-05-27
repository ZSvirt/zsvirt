package org.zstack.premium.externalservice.fluentbit;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.externalservice.ExternalServiceFactory;
import org.zstack.core.externalservice.ExternalServiceManager;
import org.zstack.core.externalservice.ExternalServiceType;
import org.zstack.core.thread.Task;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountResourceRefVO;
import org.zstack.header.identity.AccountResourceRefVO_;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.log.LogConfigurationFactory;
import org.zstack.log.LogConfigurationStruct;
import org.zstack.log.server.LogServerVO;
import org.zstack.premium.externalservice.fluentbit.output.ConfigurationStruct;
import org.zstack.premium.externalservice.fluentbit.output.FluentBitOutputFactory;
import org.zstack.premium.externalservice.fluentbit.output.OutPutType;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;
import org.zstack.utils.network.NetworkUtils;

import java.util.HashMap;
import java.util.Map;

import static org.zstack.core.Platform.*;

public class FluentBitServerFactory implements ExternalServiceFactory, Component, ManagementNodeReadyExtensionPoint, LogConfigurationFactory {
    public static final ExternalServiceType type = new ExternalServiceType("FluentBitServer");
    private static final CLogger logger = Utils.getLogger(FluentBitServerFactory.class);
    @Autowired
    private ExternalServiceManager manager;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EncryptFacade encryptFacade;

    private FluentBitServerImpl fluentBitInstance;
    public static final String FLUENT_BIT = "fluentbit";

    private Map<String, FluentBitOutputFactory> fluentBitOutputFactories = new HashMap<>();

    @Override
    public String getExternalServiceType() {
        return type.toString();
    }

    @Override
    public void managementNodeReady() {
        if (CoreGlobalProperty.UNIT_TEST_ON && !Platform.isSimulatorOn()) {
            return;
        }

        if (!FluentBitServiceUnitConfig.isInstalled()) {
            logger.warn("FluentBitServer is not installed");
            return;
        }

        thdf.submit(new Task<Void>() {
            @Override
            public String getName() {
                return "Management node joins, start fluentbit server.";
            }

            @Override
            public Void call() throws Exception {
                fluentBitInstance.start();
                return null;
            }
        });
    }

    @Override
    public boolean start() {
        populateExtensions();

         if (CoreGlobalProperty.UNIT_TEST_ON && !Platform.isSimulatorOn()) {
            return true;
        }

        fluentBitInstance = (FluentBitServerImpl) manager.getService(new FluentBitServerImpl().getName(), FluentBitServerImpl::new);
        fluentBitInstance.setEncryptFacade(encryptFacade);
        fluentBitInstance.reload();
        return true;
    }

    private void populateExtensions() {
        for (FluentBitOutputFactory f : pluginRgty.getExtensionList(FluentBitOutputFactory.class)) {
            OutPutType type = f.getFluentBitOutputType();

            FluentBitOutputFactory old = fluentBitOutputFactories.get(type.toString());

            if (old != null) {
                throw new CloudRuntimeException(String.format("duplicate FluentBitOutputFactory[%s, %s] for type[%s]",
                        f.getClass().getName(), old.getClass().getName(), old.getFluentBitOutputType().toString()));
            }

            fluentBitOutputFactories.put(type.toString(), f);
        }
    }

    @Override
    public boolean stop() {
        if (CoreGlobalProperty.UNIT_TEST_ON && !Platform.isSimulatorOn()) {
            return true;
        }

        fluentBitInstance.stop();
        return true;
    }

    @Override
    public void createLogConfiguration(LogConfigurationStruct struct, ReturnValueCompletion<String> completion) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            completion.success(struct.getConfiguration());
            return;
        }

        logger.debug(String.format("create fluentbit configuration on managementNode[uuid:%s]", Platform.getManagementServerId()));
        FluentBitConfigurationStruct fstruct = FluentBitConfigurationStruct.fromJson(struct.getConfiguration());

        FluentBitOutputFactory factory = fluentBitOutputFactories.get(fstruct.getType());

        if (factory == null) {
            throw new OperationFailureException(operr("No factory found for fluentbit output type:%s.", fstruct.getType()));
        }

        String accountUuid = Q.New(AccountResourceRefVO.class)
                        .select(AccountResourceRefVO_.accountUuid)
                        .eq(AccountResourceRefVO_.resourceUuid, struct.getUuid())
                        .findValue();

        LogServerVO serverVO = factory.createFluentBitOutput(fstruct, accountUuid, struct.getUuid(), encryptFacade);
        dbf.updateAndRefresh(serverVO);

        fluentBitInstance.restartServer();
        ErrorCode errorCode = fluentBitInstance.checkServiceReady();
        if (errorCode == null) {
            completion.success(struct.getConfiguration());
        } else {
            FluentBitServiceUnitConfig.removeOutputConfig(fstruct.getConfiguration());
            completion.fail(errorCode);
        }
    }

    @Override
    public void deleteLogConfiguration(String uuid) {
        if (CoreGlobalProperty.UNIT_TEST_ON) {
            return;
        }

        LogServerVO vo = dbf.findByUuid(uuid, LogServerVO.class);
        FluentBitConfigurationStruct fstruct = FluentBitConfigurationStruct.fromJson(vo.getConfiguration());

        FluentBitOutputFactory factory = fluentBitOutputFactories.get(fstruct.getType());

        if (factory == null) {
            throw new OperationFailureException(operr("No factory found for fluentbit output type:%s.", fstruct.getType()));
        }

        FluentBitServiceUnitConfig.removeOutputConfig(fstruct.getConfiguration());

        fluentBitInstance.restartServer();
        fluentBitInstance.checkServiceReady();
    }

    @Override
    public void loadConfiguration() {
    }

    @Override
    public String getLogConfigurationType() {
        return FLUENT_BIT;
    }

    @Override
    public void validate(LogConfigurationStruct struct) {
        FluentBitConfigurationStruct fstruct = FluentBitConfigurationStruct.fromJson(struct.getConfiguration());

        FluentBitOutputFactory factory = fluentBitOutputFactories.get(fstruct.getType());

        if (factory == null) {
            throw new ApiMessageInterceptionException(argerr("Unknown fluentbit output type %s", fstruct.getType()));
        }

        ConfigurationStruct configurationStruct = JSONObjectUtil.toObject(fstruct.getConfiguration(), ConfigurationStruct.class);
        if (configurationStruct.getHost() == null) {
            throw new ApiMessageInterceptionException(argerr("host can not be null"));
        }

        if (!CoreGlobalProperty.UNIT_TEST_ON) {
            if (!NetworkUtils.isReachable(configurationStruct.getHost(), 1000)) {
                throw new ApiMessageInterceptionException(argerr("log server [address: %s] is not available", configurationStruct.getHost()));
            }
        }

        if (configurationStruct.getPort() == null) {
            throw new ApiMessageInterceptionException(argerr("port can not be null"));
        }

        factory.validate(fstruct);
    }
}
