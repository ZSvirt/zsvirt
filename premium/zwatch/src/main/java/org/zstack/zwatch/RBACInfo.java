package org.zstack.zwatch;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.header.search.SearchConstant;
import org.zstack.zwatch.api.APIGetAlarmDataMsg;
import org.zstack.zwatch.api.APIGetAllEventMetadataMsg;
import org.zstack.zwatch.api.APIGetAllMetricMetadataMsg;
import org.zstack.zwatch.api.APIGetAuditDataMsg;
import org.zstack.zwatch.api.APIGetEventDataMsg;
import org.zstack.zwatch.api.APIGetMetricDataMsg;
import org.zstack.zwatch.api.APIQueryAlarmRecordMsg;
import org.zstack.zwatch.api.APIQueryAuditMsg;
import org.zstack.zwatch.api.APIQueryEventRecordMsg;
import org.zstack.zwatch.thirdparty.Constants;
import org.zstack.zwatch.thirdparty.api.APIQuerySNSEndpointThirdpartyAlertHistoryMsg;
import org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyAlertMsg;
import org.zstack.zwatch.thirdparty.api.APIQueryThirdpartyPlatformMsg;

import org.zstack.zwatch.alarm.APIAckAlarmDataMsg;
import org.zstack.zwatch.alarm.APIAckEventDataMsg;
import org.zstack.zwatch.alarm.APIAddActionToAlarmMsg;
import org.zstack.zwatch.alarm.APIAddActionToEventSubscriptionMsg;
import org.zstack.zwatch.alarm.APIAddLabelToAlarmMsg;
import org.zstack.zwatch.alarm.APIAddLabelToEventSubscriptionMsg;
import org.zstack.zwatch.alarm.APIChangeAlarmStateMsg;
import org.zstack.zwatch.alarm.APIChangeEventSubscriptionStateMsg;
import org.zstack.zwatch.alarm.APICreateAlarmMsg;
import org.zstack.zwatch.alarm.APIDeleteAlarmMsg;
import org.zstack.zwatch.alarm.APIGetTextTemplateArgMsg;
import org.zstack.zwatch.alarm.APIQueryAlarmMsg;
import org.zstack.zwatch.alarm.APIQueryAlertDataAckMsg;
import org.zstack.zwatch.alarm.APIQueryEventSubscriptionMsg;
import org.zstack.zwatch.alarm.APIRemoveActionFromAlarmMsg;
import org.zstack.zwatch.alarm.APIRemoveActionFromEventSubscriptionMsg;
import org.zstack.zwatch.alarm.APIRemoveLabelFromAlarmMsg;
import org.zstack.zwatch.alarm.APIRemoveLabelFromEventSubscriptionMsg;
import org.zstack.zwatch.alarm.APISubscribeEventMsg;
import org.zstack.zwatch.alarm.APIUnsubscribeEventMsg;
import org.zstack.zwatch.alarm.APIUpdateAlarmLabelMsg;
import org.zstack.zwatch.alarm.APIUpdateAlarmMsg;
import org.zstack.zwatch.alarm.APIUpdateAlertDataAckMsg;
import org.zstack.zwatch.alarm.APIUpdateEventSubscriptionLabelMsg;
import org.zstack.zwatch.alarm.APIUpdateSubscribeEventMsg;
import org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmMsg;
import org.zstack.zwatch.alarm.activealarm.api.APIQueryActiveAlarmTemplateMsg;
import org.zstack.zwatch.alarm.sns.APICreateSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.APIDeleteSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.APIQuerySNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.APIUpdateSNSTextTemplateMsg;
import org.zstack.zwatch.alarm.sns.template.aliyunsms.APIQueryAliyunSmsSNSTextTemplateMsg;
import org.zstack.zwatch.api.APICreateMetricDataHttpReceiverMsg;
import org.zstack.zwatch.api.APICreateMetricTemplateMsg;
import org.zstack.zwatch.api.APIDeleteMetricDataHttpReceiverMsg;
import org.zstack.zwatch.api.APIDeleteMetricTemplateMsg;
import org.zstack.zwatch.api.APIQueryMetricDataHttpReceiverMsg;
import org.zstack.zwatch.api.APIQueryMetricTemplateMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryEventRuleTemplateMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMetricRuleTemplateMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupAlarmMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupEventSubscriptionMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupInstanceMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorGroupTemplateRefMsg;
import org.zstack.zwatch.monitorgroup.api.APIQueryMonitorTemplateMsg;
@SDKPackage(packageName = "org.zstack.sdk.zwatch")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "zwatch";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .actions(
                    APIGetAuditDataMsg.class,
                    APIQueryAuditMsg.class,
                    APIGetAlarmDataMsg.class,
                    APIQueryAlarmRecordMsg.class,
                    APIGetMetricDataMsg.class,
                    APIGetEventDataMsg.class,
                    APIQueryEventRecordMsg.class,
                    APIGetAllEventMetadataMsg.class,
                    APIGetAllMetricMetadataMsg.class
                )
                .toOtherRole()
                .build();

        roleContributorBuilder()
                .actionsInThisPermission()
                .roleName("legacy")
                .build();

        roleBuilder()
                .uuid("d28afbf142b84fffbec9c59dc9555a05")
                .permissionBaseOnThis()
                .build();

        apis()
                .inPackage("org.zstack.zwatch.thirdparty.api")
                .toService(Constants.SERVICE_ID)
                .build();

        apis()
                .api(
                        APIQuerySNSEndpointThirdpartyAlertHistoryMsg.class,
                        APIQueryThirdpartyAlertMsg.class,
                        APIQueryThirdpartyPlatformMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIQueryAlarmMsg.class,
                        APIQueryAlertDataAckMsg.class,
                        APIQueryEventSubscriptionMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .api(
                        APIAckAlarmDataMsg.class,
                        APIAckEventDataMsg.class,
                        APIGetTextTemplateArgMsg.class,
                        APIUpdateAlertDataAckMsg.class
                )
                .toService("zwatch")
                .build();
        apis()
                .api(
                        APIAddActionToAlarmMsg.class,
                        APIAddActionToEventSubscriptionMsg.class,
                        APIAddLabelToAlarmMsg.class,
                        APIAddLabelToEventSubscriptionMsg.class,
                        APIChangeAlarmStateMsg.class,
                        APIChangeEventSubscriptionStateMsg.class,
                        APICreateAlarmMsg.class,
                        APIDeleteAlarmMsg.class,
                        APIRemoveActionFromAlarmMsg.class,
                        APIRemoveActionFromEventSubscriptionMsg.class,
                        APIRemoveLabelFromAlarmMsg.class,
                        APIRemoveLabelFromEventSubscriptionMsg.class,
                        APISubscribeEventMsg.class,
                        APIUnsubscribeEventMsg.class,
                        APIUpdateAlarmLabelMsg.class,
                        APIUpdateAlarmMsg.class,
                        APIUpdateEventSubscriptionLabelMsg.class,
                        APIUpdateSubscribeEventMsg.class
                )
                .toService("zwatch.alarm")
                .build();
        apis()
                .inPackage("org.zstack.zwatch.alarm.activealarm.api")
                .toService("zwatch.activeAlarm")
                .build();
        apis()
                .api(
                        APIQueryActiveAlarmMsg.class,
                        APIQueryActiveAlarmTemplateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .api(
                        APICreateSNSTextTemplateMsg.class,
                        APIDeleteSNSTextTemplateMsg.class,
                        APIUpdateSNSTextTemplateMsg.class
                )
                .toService("alarm.sns")
                .build();
        apis()
                .api(APIQuerySNSTextTemplateMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.zwatch.alarm.sns.template.aliyunsms")
                .toService("alarm.sns")
                .build();
        apis()
                .api(APIQueryAliyunSmsSNSTextTemplateMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.zwatch.api")
                .toService("zwatch")
                .build();
        apis()
                .api(
                        APIQueryAlarmRecordMsg.class,
                        APIQueryAuditMsg.class,
                        APIQueryEventRecordMsg.class,
                        APIQueryMetricDataHttpReceiverMsg.class,
                        APIQueryMetricTemplateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .api(
                        APICreateMetricDataHttpReceiverMsg.class,
                        APICreateMetricTemplateMsg.class,
                        APIDeleteMetricDataHttpReceiverMsg.class,
                        APIDeleteMetricTemplateMsg.class
                )
                .toService("zwatch.metricPush")
                .build();
        apis()
                .inPackage("org.zstack.zwatch.monitorgroup.api")
                .toService("zwatch.monitorGroup")
                .build();
        apis()
                .api(
                        APIQueryEventRuleTemplateMsg.class,
                        APIQueryMetricRuleTemplateMsg.class,
                        APIQueryMonitorGroupAlarmMsg.class,
                        APIQueryMonitorGroupEventSubscriptionMsg.class,
                        APIQueryMonitorGroupInstanceMsg.class,
                        APIQueryMonitorGroupMsg.class,
                        APIQueryMonitorGroupTemplateRefMsg.class,
                        APIQueryMonitorTemplateMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
