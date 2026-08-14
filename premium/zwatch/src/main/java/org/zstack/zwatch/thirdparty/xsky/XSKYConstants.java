package org.zstack.zwatch.thirdparty.xsky;

/**
 * author:kaicai.hu
 * Date:2022/9/7
 */
public interface XSKYConstants {
    String XSKY = "XSKY";

    String XSKY_MESSAGE_SOURCE_UUID = "427be35fbee34cb1aa84d0ff22d7d2f9";

    String XSKY_MESSAGE_ALARM_UUID  = "95ea13a5a91248518eb357c305a1444b";

    String XSKY_INFO_PATH = "/usr/local/hyperconverged/license/sds.info";

    String XSKY_TOKEN_PATH = "/usr/local/hyperconverged/license/sds.token";

    String XSKY_MESSAGE_TEMPLATE = "{\n" +
            "    \"product\":\"Ceph 企业版\",\n" +
            "    \"service\":\"Ceph 企业版\",\n" +
            "    \"message\":\"${resource_type + '[' + resource_name+'] ' + group + ' ' + alert_value}\",\n" +
            "    \"metric\":\"${resource_type + '::' + group}\",\n" +
            "    \"alertLevel\":\"${level == 'info' ? 'Normal' : level == 'warning' ? 'Important' : 'Emergent'}\",\n" +
            "    \"alertTime\":\"${create}\",\n" +
            "    \"dimensions\":\"{'resource_name':'${resource_name}'}\",\n" +
            "    \"dataSource\":\"Ceph 企业版\"\n" +
            "}";
}
