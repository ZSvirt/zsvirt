package org.zstack.snmp.agent;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;

/**
 * @author : jingwang
 * @date 2023/8/28 17:45
 */
@RestRequest(
        path = "/snmp/agent",
        method = HttpMethod.POST,
        responseClass = APICreateSnmpAgentEvent.class,
        parameterName = "params"
)
public class APICreateSnmpAgentMsg extends APICreateMessage implements APIAuditor, NewSnmpAgentMessage {
    @APIParam(validValues = {"v2c", "v3"})
    private String version;
    @APIParam(required = false, maxLength = 32, minLength = 1)
    private String readCommunity;
    @APIParam(required = false, maxLength = 32, minLength = 1)
    private String userName;
    @APIParam(validValues = {"MD5", "SHA", "SHA224", "SHA256", "SHA384", "SHA512"}, required = false)
    private String authAlgorithm;
    @NoLogging
    @APIParam(required = false)
    private String authPassword;
    @APIParam(validValues = {"DES", "AES128", "AES192", "AES256", "3DES"}, required = false)
    private String privacyAlgorithm;
    @NoLogging
    @APIParam(required = false)
    private String privacyPassword;
    @APIParam(numberRange = {1024, 65535})
    private int port;

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateSnmpAgentEvent) rsp).getInventory().getUuid() : "", SnmpAgentVO.class);
    }

    public static APICreateSnmpAgentMsg __example__() {
        APICreateSnmpAgentMsg msg = new APICreateSnmpAgentMsg();
        msg.setVersion(SnmpAgentVersion.v3.name());
        msg.setUserName("zstack");
        msg.setAuthAlgorithm(SnmpAgentAuthAlgorithm.SHA512.name());
        msg.setAuthPassword("auth_password");
        msg.setPrivacyAlgorithm(SnmpAgentPrivacyAlgorithm.DES.name);
        msg.setPrivacyPassword("priv_password");
        msg.setPort(161);
        return msg;
    }

    @Override
    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Override
    public String getReadCommunity() {
        return readCommunity;
    }

    public void setReadCommunity(String readCommunity) {
        this.readCommunity = readCommunity;
    }

    @Override
    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    @Override
    public String getAuthAlgorithm() {
        return authAlgorithm;
    }

    public void setAuthAlgorithm(String authAlgorithm) {
        this.authAlgorithm = authAlgorithm;
    }

    @Override
    public String getAuthPassword() {
        return authPassword;
    }

    public void setAuthPassword(String authPassword) {
        this.authPassword = authPassword;
    }

    @Override
    public String getPrivacyAlgorithm() {
        return privacyAlgorithm;
    }

    public void setPrivacyAlgorithm(String privacyAlgorithm) {
        this.privacyAlgorithm = privacyAlgorithm;
    }

    @Override
    public String getPrivacyPassword() {
        return privacyPassword;
    }

    public void setPrivacyPassword(String privacyPassword) {
        this.privacyPassword = privacyPassword;
    }

    @Override
    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }
}
