package org.zstack.sns;

import org.zstack.header.description.PackageDescription;
import org.zstack.header.rest.SDKPackage;
import org.zstack.sns.platform.email.APISNSEmailTestConnectionMsg;

import org.zstack.header.search.SearchConstant;
import org.zstack.sns.platform.dingtalk.APIQuerySNSDingTalkAtPersonMsg;
import org.zstack.sns.platform.dingtalk.APIQuerySNSDingTalkEndpointMsg;
import org.zstack.sns.platform.email.APIQuerySNSEmailAddressMsg;
import org.zstack.sns.platform.email.APIQuerySNSEmailEndpointMsg;
import org.zstack.sns.platform.email.APIQuerySNSEmailPlatformMsg;
import org.zstack.sns.platform.feishu.APIQuerySNSFeiShuAtPersonMsg;
import org.zstack.sns.platform.feishu.APIQuerySNSFeiShuEndpointMsg;
import org.zstack.sns.platform.http.APIQuerySNSHttpEndpointMsg;
import org.zstack.sns.platform.microsoftteams.APIQuerySNSMicrosoftTeamsEndpointMsg;
import org.zstack.sns.platform.snmp.APIQuerySNSSnmpPlatformMsg;
import org.zstack.sns.platform.wecom.APIQuerySNSWeComAtPersonMsg;
import org.zstack.sns.platform.wecom.APIQuerySNSWeComEndpointMsg;
@SDKPackage(packageName = "org.zstack.sdk.sns")
public class RBACInfo implements PackageDescription {
    @Override
    public String permissionName() {
        return "sns";
    }

    {
        permissionBuilder()
                .communityAvailable()
                .zsvProAvailable()
                .build();

        roleContributorBuilder()
                .roleName("zwatch")
                .actionsInThisPermission()
                .build();

        roleContributorBuilder()
                .toOtherRole()
                .actions(
                    APIQuerySNSApplicationPlatformMsg.class,
                    APISNSEmailTestConnectionMsg.class
                )
                .build();

        roleContributorBuilder()
                .actionsInThisPermission()
                .roleName("legacy")
                .build();

        roleBuilder()
                .uuid("001adb2ef25e41b7bd01b28651fcfa6a")
                .permissionBaseOnThis()
                .build();
        apis()
                .api(
                        APIQuerySNSApplicationEndpointMsg.class,
                        APIQuerySNSApplicationPlatformMsg.class,
                        APIQuerySNSSmsEndpointMsg.class,
                        APIQuerySNSTopicMsg.class,
                        APIQuerySNSTopicSubscriberMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();

        apis()
                .api(
                        APIAddSNSSmsReceiverMsg.class,
                        APIChangeSNSApplicationEndpointStateMsg.class,
                        APIChangeSNSApplicationPlatformStateMsg.class,
                        APIChangeSNSTopicStateMsg.class,
                        APICreateSNSTopicMsg.class,
                        APIDeleteSNSApplicationEndpointMsg.class,
                        APIDeleteSNSApplicationPlatformMsg.class,
                        APIDeleteSNSTopicMsg.class,
                        APIRemoveSNSSmsReceiverMsg.class,
                        APISubscribeSNSTopicMsg.class,
                        APIUnsubscribeSNSTopicMsg.class,
                        APIUpdateSNSApplicationEndpointMsg.class,
                        APIUpdateSNSApplicationPlatformMsg.class,
                        APIUpdateSNSTopicMsg.class
                )
                .toService("sns")
                .build();

        apis()
                .inPackage("org.zstack.sns.platform.dingtalk")
                .toService("sns")
                .build();
        apis()
                .api(
                        APIQuerySNSDingTalkAtPersonMsg.class,
                        APIQuerySNSDingTalkEndpointMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.email")
                .toService("sns")
                .build();
        apis()
                .api(
                        APIQuerySNSEmailAddressMsg.class,
                        APIQuerySNSEmailEndpointMsg.class,
                        APIQuerySNSEmailPlatformMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.feishu")
                .toService("sns")
                .build();
        apis()
                .api(
                        APIQuerySNSFeiShuAtPersonMsg.class,
                        APIQuerySNSFeiShuEndpointMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.http")
                .toService("sns")
                .build();
        apis()
                .api(APIQuerySNSHttpEndpointMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.microsoftteams")
                .toService("sns")
                .build();
        apis()
                .api(APIQuerySNSMicrosoftTeamsEndpointMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.snmp")
                .toService("sns")
                .build();
        apis()
                .api(APIQuerySNSSnmpPlatformMsg.class)
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
        apis()
                .inPackage("org.zstack.sns.platform.wecom")
                .toService("sns")
                .build();
        apis()
                .api(
                        APIQuerySNSWeComAtPersonMsg.class,
                        APIQuerySNSWeComEndpointMsg.class
                )
                .toService(SearchConstant.QUERY_FACADE_SERVICE_ID)
                .build();
    }
}
