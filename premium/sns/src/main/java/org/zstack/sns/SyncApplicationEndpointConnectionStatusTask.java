package org.zstack.sns;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.ResourceDestinationMaker;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.sns.platform.dingtalk.SNSDingTalkEndpointVO;
import org.zstack.sns.platform.dingtalk.SNSDingTalkEndpointVO_;
import org.zstack.sns.platform.email.SNSEmailEndpointVO;
import org.zstack.sns.platform.email.SNSEmailEndpointVO_;
import org.zstack.sns.platform.email.SNSEmailPlatformVO;
import org.zstack.sns.platform.email.SNSEmailPlatformVO_;
import org.zstack.sns.platform.feishu.SNSFeiShuEndpointVO;
import org.zstack.sns.platform.feishu.SNSFeiShuEndpointVO_;
import org.zstack.sns.platform.http.SNSHttpEndpointVO;
import org.zstack.sns.platform.http.SNSHttpEndpointVO_;
import org.zstack.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointVO;
import org.zstack.sns.platform.microsoftteams.SNSMicrosoftTeamsEndpointVO_;
import org.zstack.sns.platform.snmp.SNSSnmpPlatformVO;
import org.zstack.sns.platform.snmp.SNSSnmpPlatformVO_;
import org.zstack.sns.platform.wecom.SNSWeComEndpointVO;
import org.zstack.sns.platform.wecom.SNSWeComEndpointVO_;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.ShellResult;
import org.zstack.utils.ShellUtils;

import javax.persistence.Tuple;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.zstack.utils.CollectionDSL.e;
import static org.zstack.utils.CollectionDSL.map;
import static org.zstack.utils.DomainUtils.getDomain;

public class SyncApplicationEndpointConnectionStatusTask implements ManagementNodeReadyExtensionPoint {

    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private ResourceDestinationMaker destinationMaker;
    private Future<Void> reportEndpointConnectionStatusTask;

    @Override
    public void managementNodeReady() {
        SNSGlobalConfig.SNS_APPLICATION_ENDPOINT_CONNECTION_STATUS_INTERVAL.installUpdateExtension((oldConfig, newConfig) -> startReportEndpointConnectionStatusTask());
        startReportEndpointConnectionStatusTask();
    }

    private void startReportEndpointConnectionStatusTask() {
        if (reportEndpointConnectionStatusTask != null) {
            reportEndpointConnectionStatusTask.cancel(true);
        }
        reportEndpointConnectionStatusTask = thdf.submitPeriodicTask(new PeriodicTask() {
            @Override
            public TimeUnit getTimeUnit() {
                return TimeUnit.MINUTES;
            }

            @Override
            public long getInterval() {
                return SNSGlobalConfig.SNS_APPLICATION_ENDPOINT_CONNECTION_STATUS_INTERVAL.value(Long.class);
            }

            @Override
            public String getName() {
                return "report-endpoint-connection-status-task";
            }

            @Override
            public void run() {
                reportEndpointConnectionStatusTask();
            }
        });
    }

    private void reportEndpointConnectionStatusTask() {
        List<Tuple> endpointList = Q.New(SNSApplicationEndpointVO.class)
                .select(SNSApplicationEndpointVO_.uuid, SNSApplicationEndpointVO_.type)
                .listTuple();

        for (Tuple endpoint : endpointList) {
            String uuid = endpoint.get(0, String.class);
            String type = endpoint.get(1, String.class);
            if (!destinationMaker.isManagedByUs(uuid)) continue;
            if (StringUtils.isBlank(type)) continue;
            String target;
            String platformUuid;
            switch (type) {
                case SNSConstants.ALIYUNSMS_PLATFORM:
                    target = AliyunSmsConstants.SMS_DOMAIN;
                    break;
                case SNSConstants.EMAIL_PLATFORM:
                    platformUuid = Q.New(SNSEmailEndpointVO.class)
                            .select(SNSEmailEndpointVO_.platformUuid)
                            .eq(SNSEmailEndpointVO_.uuid, uuid)
                            .findValue();
                    if (StringUtils.isBlank(platformUuid)) continue;
                    target = Q.New(SNSEmailPlatformVO.class)
                            .select(SNSEmailPlatformVO_.smtpServer)
                            .eq(SNSEmailPlatformVO_.uuid, platformUuid)
                            .findValue();
                    break;
                case SNSConstants.DINGTALK_PLATFORM:
                    target = Q.New(SNSDingTalkEndpointVO.class)
                            .select(SNSDingTalkEndpointVO_.url)
                            .eq(SNSDingTalkEndpointVO_.uuid, uuid)
                            .findValue();
                    break;
                case SNSConstants.FEISHU_PLATFORM:
                    target = Q.New(SNSFeiShuEndpointVO.class)
                            .select(SNSFeiShuEndpointVO_.url)
                            .eq(SNSFeiShuEndpointVO_.uuid, uuid)
                            .findValue();
                    break;
                case SNSConstants.WECOM_PLATFORM:
                    target = Q.New(SNSWeComEndpointVO.class)
                            .select(SNSWeComEndpointVO_.url)
                            .eq(SNSWeComEndpointVO_.uuid, uuid)
                            .findValue();
                    break;
                case SNSConstants.MICROSOFT_TEAMS_PLATFORM:
                    target = Q.New(SNSMicrosoftTeamsEndpointVO.class)
                            .select(SNSMicrosoftTeamsEndpointVO_.url)
                            .eq(SNSMicrosoftTeamsEndpointVO_.uuid, uuid)
                            .findValue();
                    break;
                case SNSConstants.HTTP_PLATFORM:
                    target = Q.New(SNSHttpEndpointVO.class)
                            .select(SNSHttpEndpointVO_.url)
                            .eq(SNSHttpEndpointVO_.uuid, uuid)
                            .findValue();
                    break;
                case SNSConstants.SNMP_PLATFORM:
                    platformUuid = Q.New(SNSApplicationEndpointVO.class)
                            .select(SNSApplicationEndpointVO_.platformUuid)
                            .eq(SNSApplicationEndpointVO_.uuid, uuid)
                            .findValue();
                    if (StringUtils.isBlank(platformUuid)) continue;
                    target = Q.New(SNSSnmpPlatformVO.class)
                            .select(SNSSnmpPlatformVO_.snmpAddress)
                            .eq(SNSSnmpPlatformVO_.uuid, platformUuid)
                            .findValue();
                    break;
                default:
                    continue;
            }

            String domain = getDomain(target);

            ShellResult rst = ShellUtils.runAndReturn(String.format("ping -c 3 -W 2 %s", domain), true);
            String connectionStatus = rst.getRetCode() == 0 ? "UP" : "DOWN";

            SQL.New(SNSApplicationEndpointVO.class)
                    .eq(SNSApplicationEndpointVO_.uuid, uuid)
                    .set(SNSApplicationEndpointVO_.connectionStatus, connectionStatus)
                    .update();
        }
    }
}
