package org.zstack.premium.externalservice.loki;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.core.externalservice.LocalServiceUnitConfig;
import org.zstack.utils.path.PathUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/9/6.
 */
public class LokiImpl03 extends AbstractLokiImpl {
    public static String version = "0.3.0";

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    @Override
    protected String getProcessName() {
        return "loki-server";
    }

    @Override
    public String getName() {
        return String.format("loki-%s-on-management-node-%s", version, Platform.getManagementServerIp());
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        // do nothing
    }

    @Override
    public LocalServiceUnitConfig getConfigurations() {
        LocalServiceUnitConfig config = new LocalServiceUnitConfig();
        String configFolder = PathUtil.parentFolder(PathUtil.findFileOnClassPath(LokiConstant.LOKI_BIN_PATH, true).getAbsolutePath());
        config.setConfigFilePath(PathUtil.join(PathUtil.parentFolder(configFolder), LokiConstant.LOKI_CONFIG_NAME));
        config.setServiceUnitPath(LokiConstant.LOKI_SERVICE_FILE_PATH);

        Map<String, String> parameters = new HashMap<>();
        String binPath = PathUtil.findFileOnClassPath(LokiConstant.LOKI_BIN_PATH, true).getAbsolutePath();

        List<String> params = new ArrayList<>();
        params.add(String.format("-config.file=%s", config.getConfigFilePath()));

        parameters.put(LokiServiceUnitConfig.templateParameters.PARAMETERS.toString(), StringUtils.join(params, " "));
        parameters.put(LokiServiceUnitConfig.templateParameters.BINARY_PATH.toString(), binPath);
        parameters.put(LokiServiceUnitConfig.templateParameters.LOKI_SERVICE_PORT.toString(), String.valueOf(LokiGlobalProperty.LOKI_SERVER_PORT));
        parameters.put(LokiServiceUnitConfig.templateParameters.LOKI_GRPC_PORT.toString(), String.valueOf(LokiGlobalProperty.LOKI_GRPC_PORT));


        config.setConfigFileContent(LokiServiceUnitConfig.makeConfigFile(parameters, LokiConstant.InitialLokiConfig));
        config.setServiceUnitContent(LokiServiceUnitConfig.makeServiceUnitFile(parameters, LokiServiceUnitConfig.LOKI_SERVICE_UNIT_TEMPLATE));
        return config;
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[]{"loki", "-config.file=", LokiConstant.LOKI_CONFIG_NAME};
    }
}
