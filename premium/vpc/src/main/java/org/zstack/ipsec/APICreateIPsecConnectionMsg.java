package org.zstack.ipsec;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.network.service.vip.VipVO;
import java.util.Arrays;
import java.util.List;

/**
 * Created by xing5 on 2016/11/3.
 */
@TagResourceType(IPsecConnectionVO.class)
@RestRequest(path = "/ipsec", method = HttpMethod.POST, responseClass = APICreateIPsecConnectionEvent.class, parameterName = "params")
public class APICreateIPsecConnectionMsg extends APICreateMessage implements APIAuditor {

    // IPSec basic configuration
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(maxLength = 2048, required = false)
    private String description;

    @APIParam(resourceType = VipVO.class)
    private String vipUuid;

    @APIParam(maxLength = 255)
    private String peerAddress;

    @APIParam(maxLength = 255, validValues = { "psk", "certs" }, required = false)
    private String authMode = "psk";

    @APIParam
    private String authKey;

    @APIParam(maxLength = 32, validValues = { "ip", "name", "fqdn" }, required = false)
    private String idType = "ip";

    @APIParam(maxLength = 255, required = false)
    private String localId;

    @APIParam(maxLength = 255, required = false)
    private String remoteId;

    // IPSec flow of interest
    @APIParam(resourceType = L3NetworkVO.class, required = false)
    private String l3NetworkUuid;

    @APIParam(nonempty = true, required = false)
    private List<String> peerCidrs;

    // ike proposal
    @APIParam(maxLength = 32, validValues = { "ike", "ikev1", "ikev2"}, required = false)
    private String ikeVersion = "ike";

    @APIParam(maxLength = 32, validValues = { "md5", "sha1", "sha256", "sha384", "sha512" }, required = false)
    private String ikeAuthAlgorithm = "sha256";

    @APIParam(maxLength = 32, validValues = { "3des", "aes-128", "aes-192", "aes-256" }, required = false)
    private String ikeEncryptionAlgorithm = "aes-256";

    private int ikeDhGroup = 2;

    @APIParam(numberRange = {60, 604800}, required = false)
    private int ikeLifeTime = 86400;

    // ipsec proposal
    @APIParam(maxLength = 32, validValues = { "md5", "sha1", "sha256", "sha384", "sha512" }, required = false)
    private String policyAuthAlgorithm = "sha256";

    @APIParam(maxLength = 32, validValues = { "3des", "aes-128", "aes-192", "aes-256" }, required = false)
    private String policyEncryptionAlgorithm = "aes-256";

    @APIParam(maxLength = 32, required = false, validValues = {"none", "dh-group0", "dh-group2", "dh-group5", "dh-group14", "dh-group15", "dh-group16", "dh-group17", "dh-group18", "dh-group19", "dh-group20", "dh-group21", "dh-group22", "dh-group23", "dh-group24", "dh-group25", "dh-group26" })
    private String pfs = "dh-group14";

    @APIParam(maxLength = 32, required = false, validValues = { "tunnel", "transport" })
    private String policyMode = "tunnel";

    @APIParam(maxLength = 32, required = false, validValues = { "esp", "ah", "ah-esp" })
    private String transformProtocol = "esp";

    @APIParam(numberRange = {30, 604800}, required = false)
    private int lifeTime = 3600;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
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

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public List<String> getPeerCidrs() {
        return peerCidrs;
    }

    public void setPeerCidrs(List<String> peerCidrs) {
        this.peerCidrs = peerCidrs;
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

    public String getIkeVersion() {
        return ikeVersion;
    }

    public void setIkeVersion(String ikeVersion) {
        this.ikeVersion = ikeVersion;
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

    public int getIkeLifeTime() {
        return ikeLifeTime;
    }

    public void setIkeLifeTime(int ikeLifeTime) {
        this.ikeLifeTime = ikeLifeTime;
    }

    public int getLifeTime() {
        return lifeTime;
    }

    public void setLifeTime(int lifeTime) {
        this.lifeTime = lifeTime;
    }

    public static APICreateIPsecConnectionMsg __example__() {
        APICreateIPsecConnectionMsg msg = new APICreateIPsecConnectionMsg();
        msg.setName("Test-IPSec");
        msg.setL3NetworkUuid(uuid());
        msg.setPeerAddress("100.64.10.10");
        msg.setAuthKey("auth");
        msg.setVipUuid(uuid());
        msg.setPeerCidrs(Arrays.asList("192.168.100.0/24"));
        msg.setIkeVersion("ikev2"); //only "ikev1" or "ikev2" 默认为"ikev2"
        msg.setIdType("ip"); //支持ip, name 默认为"ip"
        msg.setLocalId("1.1.1.1"); // maxLength = 255 ,默认值为本端ip(vip)
        msg.setRemoteId("2.2.2.2"); // maxLength = 255 ,默认值为对端ip
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateIPsecConnectionEvent)rsp).getInventory().getUuid() : "", IPsecConnectionVO.class);
    }
}
