package org.zstack.sns.platform.snmp;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;
import org.zstack.sns.APIUpdateSNSApplicationPlatformEvent;
import org.zstack.sns.APIUpdateSNSApplicationPlatformMsg;

/**
 * @Author : jingwang
 * @create 2023/8/24 13:26
 */
@RestRequest(
        path = "/sns/application-platforms/snmp/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSNSApplicationPlatformEvent.class,
        isAction = true
)
public class APIUpdateSNSSnmpPlatformMsg extends APIUpdateSNSApplicationPlatformMsg {
    @APIParam
    private String snmpAddress;
    @APIParam(numberRange = {1, 65535})
    private Integer snmpPort;

    public static APIUpdateSNSSnmpPlatformMsg __example__() {
        APIUpdateSNSSnmpPlatformMsg msg = new APIUpdateSNSSnmpPlatformMsg();
        msg.setUuid(uuid());
        msg.setName("snmp platform name");
        msg.setSnmpAddress("127.0.0.1");
        msg.setSnmpPort(161);
        return msg;
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
