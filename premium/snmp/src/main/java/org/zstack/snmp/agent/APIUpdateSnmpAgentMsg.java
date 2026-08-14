package org.zstack.snmp.agent;

import org.springframework.http.HttpMethod;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.*;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.snmp.SnmpAgentInnerMessage;
import org.zstack.snmp.SnmpAgentLocalMessageBuilder;

/**
 *
 * @Author : jingwang
 * @create 2023/7/14 4:22 PM
 */
@RestRequest(
        path = "/snmp/agent/actions",
        method = HttpMethod.PUT,
        responseClass = APIUpdateSnmpAgentEvent.class,
        isAction = true
)
public class APIUpdateSnmpAgentMsg extends APIMessage implements APIAuditor, SnmpAgentLocalMessageBuilder, NewSnmpAgentMessage {
    @APIParam(resourceType = SnmpAgentVO.class)
    private String uuid;
    @APIParam(validValues = {"v2c","v3"})
    private String version;
    @APIParam(required = false, maxLength = 32, minLength = 1)
    private String readCommunity;
    @APIParam(required = false, maxLength = 32, minLength = 1)
    private String userName;
    @APIParam(validValues = {"MD5","SHA","SHA224","SHA256","SHA384","SHA512"}, required = false)
    private String authAlgorithm;
    @NoLogging
    @APIParam(required = false)
    private String authPassword;
    @APIParam(validValues = {"DES","AES128","AES192","AES256","3DES"}, required = false)
    private String privacyAlgorithm;
    @NoLogging
    @APIParam(required = false)
    private String privacyPassword;
    @APIParam(numberRange = {1024, 65535})
    private int port;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(((APIUpdateSnmpAgentMsg) msg).getUuid(), SnmpAgentVO.class);
    }

    public static APIUpdateSnmpAgentMsg __example__() {
        APIUpdateSnmpAgentMsg msg = new APIUpdateSnmpAgentMsg();
        msg.setVersion("v2c");
        msg.setReadCommunity("zstack");
        msg.setPort(1161);
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

    @Override
    public SnmpAgentInnerMessage buildLocalMessage() {
        UpdateSnmpAgentMsg msg = new UpdateSnmpAgentMsg();
        SnmpAgentVO vo = SnmpAgentUtils.buildSnmpAgentVOFromNewSnmpAgentMessage(this, getUuid());
        msg.setAgentVO(vo);
        return msg;
    }
}
