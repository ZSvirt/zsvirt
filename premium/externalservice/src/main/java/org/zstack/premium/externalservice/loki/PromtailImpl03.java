package org.zstack.premium.externalservice.loki;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.core.externalservice.LocalServiceUnitConfig;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.ManagementNodeVO;
import org.zstack.header.managementnode.ManagementNodeVO_;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mingjian.deng on 2019/9/9.
 */
public class PromtailImpl03 extends AbstractLokiImpl {
    public static String version = "0.3.0";
    public String localIp;

    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    public PromtailImpl03(String localIp) {
        this.localIp = localIp;
    }

    @Override
    protected String getProcessName() {
        return "promtail-server";
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[]{"promtail", "-config.file=", LokiConstant.PROMTAIL_CONFIG_NAME};
    }

    @Override
    public String getName() {
        return String.format("promtail-%s", version);
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
        config.setConfigFilePath(LokiConstant.PROMTAIL_CONFIG_NAME);
        config.setServiceUnitPath(LokiConstant.PROMTAIL_SERVICE_FILE_PATH);

        Map<String, String> parameters = new HashMap<>();
        String binPath = LokiConstant.PROMTAIL_BIN_PATH;

        List<String> params = new ArrayList<>();
        params.add(String.format("-config.file=%s", config.getConfigFilePath()));

        parameters.put(LokiServiceUnitConfig.templateParameters.PARAMETERS.toString(), StringUtils.join(params, " "));
        parameters.put(LokiServiceUnitConfig.templateParameters.BINARY_PATH.toString(), binPath);

        parameters.put(LokiServiceUnitConfig.templateParameters.PROMTAIL_PORT.toString(), String.valueOf(LokiGlobalProperty.PROMTAIL_PORT));
        parameters.put(LokiServiceUnitConfig.templateParameters.LOCAL_IP.toString(), localIp);
        parameters.put(LokiServiceUnitConfig.templateParameters.ZSTACK_HOME.toString(), System.getProperty("user.home"));

        List<String> mnIps = Q.New(ManagementNodeVO.class).select(ManagementNodeVO_.hostName).listValues();
        if (mnIps.isEmpty()) {
            throw new OperationFailureException(Platform.inerr("no management found!"));
        }

        StringBuilder url = new StringBuilder();
        for (String mnIp: mnIps) {
            url.append(String.format("- url: http://%s:%s/api/prom/push", mnIp, String.valueOf(LokiGlobalProperty.LOKI_SERVER_PORT)));
        }
        parameters.put(LokiServiceUnitConfig.templateParameters.LOKI_API_URL.toString(), url.toString());

        config.setServiceUnitContent(LokiServiceUnitConfig.makeServiceUnitFile(parameters, LokiServiceUnitConfig.PROMTAIL_UNIT_TEMPLATE));
        config.setConfigFileContent(LokiServiceUnitConfig.makeConfigFile(parameters, LokiConstant.InitialPromtailConfig));
        return config;
    }
}
