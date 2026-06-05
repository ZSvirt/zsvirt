package org.zstack.premium.externalservice.fluentbit;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.CoreGlobalProperty;
import org.zstack.core.Platform;
import org.zstack.core.db.Q;
import org.zstack.core.encrypt.EncryptFacade;
import org.zstack.core.externalservice.AbstractLocalExternalService;
import org.zstack.core.externalservice.ExternalServiceCapabilitiesBuilder;
import org.zstack.core.retry.Retry;
import org.zstack.core.retry.RetryCondition;
import org.zstack.header.core.external.service.ExternalServiceCapabilities;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.log.server.LogCategory;
import org.zstack.log.server.LogServerVO;
import org.zstack.log.server.LogServerVO_;
import org.zstack.premium.externalservice.fluentbit.output.FluentBitOutputConfigHelper;
import org.zstack.premium.externalservice.fluentbit.output.OutPutType;
import org.zstack.utils.Bash;
import org.zstack.utils.Utils;
import org.zstack.utils.gson.JSONObjectUtil;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.*;

public class FluentBitServerImpl extends AbstractLocalExternalService implements FluentBitServer {
    ExternalServiceCapabilities capabilities = ExternalServiceCapabilitiesBuilder
            .build()
            .reloadConfig(false);

    private boolean isReady = false;
    private static final CLogger logger = Utils.getLogger(FluentBitServerImpl.class);
    private EncryptFacade encryptFacade;

    public void setEncryptFacade(EncryptFacade encryptFacade) {
        this.encryptFacade = encryptFacade;
    }

    @Override
    public String getName() {
        return String.format("fluentbit-log-server-on-management-node-%s", Platform.getManagementServerIp());
    }

    public String getServiceType() {return "FluentBitServer";}

    public String getSystemdServiceName() {
        return FluentBitServiceUnitConfig.serviceName;
    }

    @Override
    protected String[] getCommandLineKeywords() {
        return new String[]{FluentBitServiceUnitConfig.binPath, FluentBitServiceUnitConfig.configFilePath};
    }

    @Override
    public void start() {
        if (!isReady) {
            prepare();
        }

        loadFluentBitConfiguration();

        if (!isNeedToStart()) {
            isReady = true;
            return;
        }

        restartServer();
        checkServiceReady();
        isReady = true;
    }

    private void loadFluentBitConfiguration() {
        FluentBitServiceUnitConfig.generateFluentBitConfig();
        List<LogServerVO> list = Q.New(LogServerVO.class)
                .eq(LogServerVO_.category, LogCategory.PlatformOperationLog)
                .list();
        for (LogServerVO vo : list) {
            try {
                FluentBitConfigurationStruct struct = FluentBitConfigurationStruct.fromJson(vo.getConfiguration());
                OutPutType type = getOutPutType(struct == null ? null : struct.getType(), vo.getUuid());
                if (type == null) {
                    continue;
                }

                String configuration = struct.getConfiguration();
                Map map = JSONObjectUtil.toObject(configuration, Map.class);
                map = FluentBitOutputConfigHelper.normalizeConfiguration(map);
                if (encryptFacade != null && map.containsKey("http_passwd")) {
                    map.put("http_passwd", encryptFacade.decrypt((String) map.get("http_passwd")));
                }
                if (encryptFacade != null && map.containsKey("httpPassword")) {
                    map.put("httpPassword", encryptFacade.decrypt((String) map.get("httpPassword")));
                }
                FluentBitServiceUnitConfig.addSingleFluentBitConfig(FluentBitOutputConfigHelper.makeOutputConfig(
                        type, map, vo.getAccountUuid()));
            } catch (Exception e) {
                logger.warn(String.format("failed to load fluent-bit log server[uuid:%s], skip it: %s",
                        vo.getUuid(), e.getMessage()), e);
            }
        }
    }

    private OutPutType getOutPutType(String type, String logServerUuid) {
        if (type == null) {
            logger.warn(String.format("fluent-bit log server[uuid:%s] contains empty output type, skip it", logServerUuid));
            return null;
        }

        try {
            return OutPutType.valueOf(type);
        } catch (IllegalArgumentException e) {
            logger.warn(String.format("fluent-bit log server[uuid:%s] contains unknown output type[%s], skip it",
                    logServerUuid, type));
            return null;
        }
    }

    private boolean isNeedToStart() {
        return FluentBitServiceUnitConfig.checkForSection(FluentBitServiceUnitConfig.configFilePath, "[OUTPUT]") || FluentBitServiceUnitConfig.checkForSection(FluentBitServiceUnitConfig.externalConfigFilePath, "[OUTPUT]");
    }

    private void sysctl(String ctl) {
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                sudoRun("systemctl %s %s", ctl, getSystemdServiceName());
            }
        }.execute();
    }

    @Override
    public void stop() {
        if (!isAlive()) {
            return;
        }
        sysctl("stop");
    }

    @Override
    public boolean isAlive() {
        return getPID() != null;
    }

    @Override
    public ExternalServiceCapabilities getExternalServiceCapabilities() {
        return capabilities;
    }

    @Override
    public void reload() {
        start();
    }

    public ErrorCode checkServiceReady() {
        try {
            new Retry<Void>() {
                @Override
                @RetryCondition(onExceptions = {CloudRuntimeException.class})
                protected Void call() {
                    doCheckServiceReady();
                    logger.info("fluentBit server is ready");
                    return null;
                }

                @Override
                protected boolean onFailure(Throwable t) {
                    logger.error(String.format("fluentBit server not ready in about 55s: %s", t.getMessage())); //5 * (INTERVAL 1 + 5 * (TIMEOUT 1 + INTERVAL 1))
                    return true;
                }
            }.run();
            return null;
        } catch (Exception e) {
            ErrorCode error;
            if (e.getMessage().contains("fluent-bit.service") && e.getMessage().contains("Active:")) {
                error = argerr("The creation of the log server failed, incorrect newly passed configuration parameters may have " +
                        "caused the Fluent Bit service to be in an inactive state. Please double-check the configuration for accuracy");
            } else {
                error = argerr("the new configuration has errors, " +
                        "causing service exceptions. Please check configuration params again," +
                        "Details: %s", e.getMessage());
            }
            logger.error(String.format("The fluent-bit service is not ready %s", e.getMessage()));
            return error;
        }
    }

    private void doCheckServiceReady() {
        new Bash() {
            @Override
            protected void scripts() {
                unsetE();
                int returnCode = sudoRun("systemctl status %s", getSystemdServiceName());
                if (returnCode != 0) {
                    sudoRun("systemctl restart %s", getSystemdServiceName());
                    returnCode = sudoRun("systemctl status %s", getSystemdServiceName());
                    if (returnCode != 0) {
                        throw new CloudRuntimeException("fluentbit server is not ready");
                    }
                }
            }
        }.execute();
    }

    public void restartServer() {
        if (CoreGlobalProperty.UNIT_TEST_ON){
            return;
        }

        logger.info("start fluentbit server");
        sysctl("restart");
        logger.info("finish to start fluentbit server");
    }

    private void prepare() {
        new Bash() {
            @Override
            protected void scripts() {
                setE();
                String username = System.getProperty("user.name");
                String configFilePath = FluentBitServiceUnitConfig.configFilePath;
                String logFilePath = FluentBitServiceUnitConfig.logFilePath;
                String serviceUnitPath = FluentBitServiceUnitConfig.serviceUnitPath;
                String externalConfigFilePath = FluentBitServiceUnitConfig.externalConfigFilePath;

                sudoRun("touch %s", configFilePath);
                sudoRun("chmod 640 %s", configFilePath);
                sudoRun("chown %s:%s %s", username, username, configFilePath);

                String secretsFilePath = FluentBitServiceUnitConfig.secretsFilePath;
                sudoRun("touch %s", secretsFilePath);
                sudoRun("chmod 600 %s", secretsFilePath);
                sudoRun("chown %s:%s %s", username, username, secretsFilePath);

                sudoRun("touch %s", logFilePath);
                sudoRun("chmod 640 %s", logFilePath);
                sudoRun("chown %s:%s %s", username, username, logFilePath);

                sudoRun("touch %s", externalConfigFilePath);
                sudoRun("chmod 644 %s", externalConfigFilePath);
                sudoRun("chown %s:%s %s", username, username, externalConfigFilePath);

                sudoRun("touch %s", serviceUnitPath);
                run("echo '%s' | sudo tee %s > /dev/null", FluentBitServiceUnitConfig.makeServiceUnitConfig(), serviceUnitPath);
                sudoRun("chown %s:%s %s", username, username, serviceUnitPath);
                sudoRun("chmod 644 %s", serviceUnitPath);

                sudoRun("systemctl daemon-reload");
            }
        }.execute();
    }
}
