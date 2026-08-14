package org.zstack.sns.platform.snmp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APICreateSNSApplicationPlatformEvent;
import org.zstack.sns.APICreateSNSApplicationPlatformMsg;
import org.zstack.sns.SNSConstants;

/**
 * @Author : jingwang
 * @create 2023/8/24 13:14
 */
@RestRequest(
        path = "/sns/application-platforms/snmp",
        method = HttpMethod.POST,
        responseClass = APICreateSNSApplicationPlatformEvent.class,
        parameterName = "params"
)
public class APICreateSNSSnmpPlatformMsg extends APICreateSNSApplicationPlatformMsg {
    @APIParam
    private String snmpAddress;
    @APIParam(numberRange = {1, 65535})
    private Integer snmpPort;

    public static APICreateSNSSnmpPlatformMsg __example__() {
        APICreateSNSSnmpPlatformMsg msg = new APICreateSNSSnmpPlatformMsg();
        msg.setName("snmp platform");
        msg.setSnmpAddress("127.0.0.1");
        msg.setSnmpPort(161);
        return msg;
    }

    @Override
    public String getType() {
        return SNSConstants.SNMP_PLATFORM;
    }

    public String getSnmpAddress() {
        return snmpAddress;
    }

    public void setSnmpAddress(String snmpAddress) {
        this.snmpAddress = snmpAddress;
    }

    public Integer getSnmpPort() {
        return snmpPort;
    }

    public void setSnmpPort(Integer snmpPort) {
        this.snmpPort = snmpPort;
    }
}
