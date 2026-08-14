package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.RestRequest;


/**
 * Created by boce.wang on 2022/8/10.
 */
@RestRequest(
        path = "/ipsec/config/{uuid}",
        method = HttpMethod.PUT,
        responseClass = APIChangeIPsecConnectionEvent.class,
        isAction = true
)
public class APIChangeIPsecConnectionMsg extends APIMessage implements IPsecConnectionMessage {
    @APIParam(resourceType = IPsecConnectionVO.class)
    private String uuid;

    @APIParam(maxLength = 255)
    private String peerAddress;

    @APIParam(maxLength = 255, validValues = {"psk", "certs"}, required = false)
    private String authMode = "psk";

    @APIParam
    private String authKey;

    @APIParam(maxLength = 32, validValues = {"ip", "name", "fqdn"}, required = false)
    private String idType = "ip";

    @APIParam(maxLength = 255, required = false)
    private String localId;

    @APIParam(maxLength = 255, required = false)
    private String remoteId;

    // ike proposal
    @APIParam(maxLength = 32, validValues = {"ike", "ikev1", "ikev2"}, required = false)
    private String ikeVersion = "ike";

    @APIParam(maxLength = 32, validValues = {"md5", "sha1", "sha256", "sha384", "sha512"}, required = false)
    private String ikeAuthAlgorithm = "sha256";

    @APIParam(maxLength = 32, validValues = {"3des", "aes-128", "aes-192", "aes-256"}, required = false)
    private String ikeEncryptionAlgorithm = "aes-256";

    private int ikeDhGroup = 2;

    @APIParam(numberRange = {60, 604800}, required = false)
    private int ikeLifeTime = 86400;

    // ipsec proposal
    @APIParam(maxLength = 32, validValues = {"md5", "sha1", "sha256", "sha384", "sha512"}, required = false)
    private String policyAuthAlgorithm = "sha256";

    @APIParam(maxLength = 32, validValues = {"3des", "aes-128", "aes-192", "aes-256"}, required = false)
    private String policyEncryptionAlgorithm = "aes-256";

    @APIParam(maxLength = 32, required = false, validValues = {"none", "dh-group2", "dh-group5", "dh-group14", "dh-group15", "dh-group16", "dh-group17", "dh-group18", "dh-group19", "dh-group20", "dh-group21", "dh-group22", "dh-group23", "dh-group24", "dh-group25", "dh-group26"})
    private String pfs = "dh-group14";

    @APIParam(maxLength = 32, required = false, validValues = {"tunnel", "transport"})
    private String policyMode = "tunnel";

    @APIParam(maxLength = 32, required = false, validValues = {"esp", "ah", "ah-esp"})
    private String transformProtocol = "esp";

    @APIParam(numberRange = {30, 604800}, required = false)
    private int lifeTime = 3600;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPeerAddress() {
        return peerAddress;
    }

    public void setPeerAddress(String peerAddress) {
        this.peerAddress = peerAddress;
    }

    public String getAuthMode() {
        return authMode;
    }

    public void setAuthMode(String authMode) {
        this.authMode = authMode;
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
    }

    public String getIdType() {
        return idType;
    }

    public void setIdType(String idType) {
        this.idType = idType;
    }

    public String getLocalId() {
        return localId;
    }

    public void setLocalId(String localId) {
        this.localId = localId;
    }

    public String getRemoteId() {
        return remoteId;
    }

    public void setRemoteId(String remoteId) {
        this.remoteId = remoteId;
    }

    public String getIkeVersion() {
        return ikeVersion;
    }

    public void setIkeVersion(String ikeVersion) {
        this.ikeVersion = ikeVersion;
    }

    public String getIkeAuthAlgorithm() {
        return ikeAuthAlgorithm;
    }

    public void setIkeAuthAlgorithm(String ikeAuthAlgorithm) {
        this.ikeAuthAlgorithm = ikeAuthAlgorithm;
    }

    public String getIkeEncryptionAlgorithm() {
        return ikeEncryptionAlgorithm;
    }

    public void setIkeEncryptionAlgorithm(String ikeEncryptionAlgorithm) {
        this.ikeEncryptionAlgorithm = ikeEncryptionAlgorithm;
    }

    public int getIkeDhGroup() {
        return ikeDhGroup;
    }

    public void setIkeDhGroup(int ikeDhGroup) {
        this.ikeDhGroup = ikeDhGroup;
    }

    public int getIkeLifeTime() {
        return ikeLifeTime;
    }

    public void setIkeLifeTime(int ikeLifeTime) {
        this.ikeLifeTime = ikeLifeTime;
    }

    public String getPolicyAuthAlgorithm() {
        return policyAuthAlgorithm;
    }

    public void setPolicyAuthAlgorithm(String policyAuthAlgorithm) {
        this.policyAuthAlgorithm = policyAuthAlgorithm;
    }

    public String getPolicyEncryptionAlgorithm() {
        return policyEncryptionAlgorithm;
    }

    public void setPolicyEncryptionAlgorithm(String policyEncryptionAlgorithm) {
        this.policyEncryptionAlgorithm = policyEncryptionAlgorithm;
    }

    public String getPfs() {
        return pfs;
    }

    public void setPfs(String pfs) {
        this.pfs = pfs;
    }

    public String getPolicyMode() {
        return policyMode;
    }

    public void setPolicyMode(String policyMode) {
        this.policyMode = policyMode;
    }

    public String getTransformProtocol() {
        return transformProtocol;
    }

    public void setTransformProtocol(String transformProtocol) {
        this.transformProtocol = transformProtocol;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    @Override
    public String getIPsecConnectionUuid() {
        return uuid;
    }

    public static APIChangeIPsecConnectionMsg __example__() {
        APIChangeIPsecConnectionMsg msg = new APIChangeIPsecConnectionMsg();

        msg.setUuid(uuid());
        msg.setPeerAddress("2.2.2.2");
        msg.setAuthMode("psk");
        msg.setAuthKey("12345678");
        msg.setIdType("ip");
        msg.setLocalId("1.1.1.1");
        msg.setRemoteId("2.2.2.2");
        msg.setIkeVersion("ikev2");
        msg.setIkeAuthAlgorithm("sha256");
        msg.setIkeEncryptionAlgorithm("aes-256");
        msg.setIkeDhGroup(2);
        msg.setIkeLifeTime(86400); //前端不展示, 默认值86400, Cli支持自定义
        msg.setPolicyAuthAlgorithm("sha256");
        msg.setPolicyEncryptionAlgorithm("aes-256");
        msg.setPfs("dh-group14");
        msg.setPolicyMode("tunnel");
        msg.setTransformProtocol("esp");
        msg.setLifeTime(3600); //前端不展示, 默认值3600, Cli支持自定义
        return msg;
    }
}
