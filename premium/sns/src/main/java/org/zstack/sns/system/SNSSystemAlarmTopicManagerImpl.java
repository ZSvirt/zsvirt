package org.zstack.sns.system;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.ChainTask;
import org.zstack.core.thread.SyncTaskChain;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.header.managementnode.PrepareDbInitialValueExtensionPoint;
import org.zstack.header.rest.RESTFacade;
import org.zstack.identity.AccountManager;
import org.zstack.sns.*;
import org.zstack.sns.platform.system.SystemPlatformGlobalProperty;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;

import static org.zstack.core.Platform.operr;

public class SNSSystemAlarmTopicManagerImpl implements Component, SNSSystemAlarmTopicManager,
        PrepareDbInitialValueExtensionPoint, BeforeDeleteSNSTopicExtensionPoint {
    private static final CLogger logger = Utils.getLogger(SNSSystemAlarmTopicManagerImpl.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private AccountManager acntMgr;
    @Autowired
    protected ThreadFacade thdf;
    @Autowired
    protected RESTFacade restf;
    @Autowired
    protected EventFacade evtf;
    @Autowired
    protected PluginRegistry pluginRgty;

    @Override
    public void prepareDbInitialValue() {
        createTopicAndEndpoint();
    }

    @Transactional
    private void createTopicAndEndpoint() {
        String systemTopicHttpEndpointURL = SystemPlatformGlobalProperty.systemTopicHttpEndpointURL;

        if (StringUtils.isEmpty(systemTopicHttpEndpointURL)) {
            logger.warn("sns.systemTopic.endpoints.http.url is not configured in zstack.properties, you may not receive system status changing messages");
        }

        String httpUsername = SystemPlatformGlobalProperty.systemTopicHttpEndpointURLUsername.equals("") ? null : SystemPlatformGlobalProperty.systemTopicHttpEndpointURLUsername;
        String httpPassword = SystemPlatformGlobalProperty.systemTopicHttpEndpointURLPassword.equals("") ? null : SystemPlatformGlobalProperty.systemTopicHttpEndpointURLPassword;

        HTTPTopicAndEndPointCreator.builder()
                .topicName(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_NAME)
                .topicDescription("topic for reporting system defined alarms")
                .topicUuid(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID)
                .endpointName(SNSSystemAlarmTopicManager.SYSTEM_ALARM_ENDPOINT_NAME)
                .endpointDescription("endpoint for reporting system defined alarms")
                .httpURL(systemTopicHttpEndpointURL)
                .httpUsername(httpUsername)
                .httpPassword(httpPassword)
                .build()
                .mergeByName();
    }

    @Override
    public void beforeDeleteSNSTopic(SNSTopicInventory topic) {
        if (topic.getUuid().equals(SNSSystemAlarmTopicManager.SYSTEM_ALARM_TOPIC_UUID)) {
            throw new OperationFailureException(operr("system alarm topic cannot be deleted"));
        }
    }

    private void setupCanonicalEvents() {
        evtf.on(SNSCanonicalEvents.UPDATE_SNS_GLOBAL_PROPERTY_PATH, new EventCallback<Object>() {
            @Override
            protected void run(Map tokens, Object data) {
                SNSCommands.UpdateSNSGlobalPropertyCmd cmd = (SNSCommands.UpdateSNSGlobalPropertyCmd) data;

                // update database and memory if necessary
                if (cmd.systemTopicHttpEndpointURL == null) {
                    return;
                }

                if (cmd.systemTopicHttpEndpointURL.equals(SystemPlatformGlobalProperty.systemTopicHttpEndpointURL)) {
                    return;
                }

                SystemPlatformGlobalProperty.systemTopicHttpEndpointURL = cmd.systemTopicHttpEndpointURL;
                createTopicAndEndpoint();
                logger.debug(String.format("updated sns.systemTopic.endpoints.http.url to %s", cmd.systemTopicHttpEndpointURL));
            }
        });
    }

    private void registerSyncHttpCallHandlers() {
        restf.registerSyncHttpCallHandler(SNSConstants.SNS_GLOBAL_PROPERTY_UPDATE_REPORTER, SNSCommands.UpdateSNSGlobalPropertyCmd.class, cmd -> {
            thdf.chainSubmit(new ChainTask(null) {
                @Override
                public String getSyncSignature() {
                    return "update-sns-global-properties";
                }

                @Override
                public void run(SyncTaskChain chain) {
                    // call sns global property update extensions
                    List<SNSGlobalPropertyExtension> extensions = pluginRgty.getExtensionList(SNSGlobalPropertyExtension.class);
                    CollectionUtils.safeForEach(extensions, ext -> ext.afterSNSGlobalPropertyUpdated(cmd));

                    // fire sns global property update event
                    evtf.fire(SNSCanonicalEvents.UPDATE_SNS_GLOBAL_PROPERTY_PATH, cmd);

                    // update database and memory if necessary
                    if (cmd.systemTopicHttpEndpointURL == null) {
                        chain.next();
                        return;
                    }

                    if (cmd.systemTopicHttpEndpointURL.equals(SystemPlatformGlobalProperty.systemTopicHttpEndpointURL)) {
                        chain.next();
                        return;
                    }

                    SystemPlatformGlobalProperty.systemTopicHttpEndpointURL = cmd.systemTopicHttpEndpointURL;
                    createTopicAndEndpoint();
                    logger.debug(String.format("updated sns.systemTopic.endpoints.http.url to %s", cmd.systemTopicHttpEndpointURL));
                    chain.next();
                }

                @Override
                public String getName() {
                    return getSyncSignature();
                }
            });

            return null;
        });
    }

    @Override
    public boolean start() {
        setupCanonicalEvents();
        registerSyncHttpCallHandlers();

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}