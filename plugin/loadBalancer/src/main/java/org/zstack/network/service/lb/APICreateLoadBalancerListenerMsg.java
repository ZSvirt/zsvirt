package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;
import org.zstack.header.acl.AccessControlListVO;

import java.util.List;

/**
 * Created by frank on 8/8/2015.
 */
@TagResourceType(LoadBalancerListenerVO.class)
@RestRequest(
        path = "/load-balancers/{loadBalancerUuid}/listeners",
        method = HttpMethod.POST,
        responseClass = APICreateLoadBalancerListenerEvent.class,
        parameterName = "params"
)
public class APICreateLoadBalancerListenerMsg extends APICreateMessage implements LoadBalancerMessage, APIAuditor {
    @APIParam(resourceType = LoadBalancerVO.class)
    private String loadBalancerUuid;
    @APIParam(maxLength = 255)
    private String name;
    @APIParam(maxLength = 2048, required = false)
    private String description;
    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer instancePort;
    @APIParam(numberRange = {1, 65535})
    private int loadBalancerPort;
    @APIParam(maxLength = 255, validValues = {LoadBalancerConstants.LB_PROTOCOL_UDP, LoadBalancerConstants.LB_PROTOCOL_TCP, LoadBalancerConstants.LB_PROTOCOL_HTTP, LoadBalancerConstants.LB_PROTOCOL_HTTPS}, required = false)
    private String protocol;
    @APIParam(resourceType = CertificateVO.class, required = false)
    private String certificateUuid;
    @APIParam(validValues = {LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP}, required = false)
    private String healthCheckProtocol;
    @APIParam(validValues = {"GET", "HEAD"}, required = false)
    private String healthCheckMethod;
    @APIParam(validRegexValues = LoadBalancerConstants.HEALTH_CHECK_URI_REGEX, maxLength = 80, required = false)
    private String healthCheckURI;
    @APIParam(maxLength = 80, required = false)
    private String healthCheckHttpCode;
    @APIParam(validValues = {"enable", "disable"}, required = false)
    private String aclStatus = LoadBalancerAclStatus.disable.toString();
    @APIParam(resourceType = AccessControlListVO.class, required = false)
    private List<String> aclUuids;
    @APIParam(validValues = {"white","black"}, required = false)
    private String aclType = LoadBalancerAclType.black.toString();
    @APIParam(validValues = {LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_DEFAULT, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_0, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_1,
            LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2_STRICT,
            LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2_STRICT_WITH_1_3}, required = false)
    private String securityPolicyType;

    @Override
    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
    }

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

    public Integer getInstancePort() {
        return instancePort;
    }

    public void setInstancePort(int instancePort) {
        this.instancePort = instancePort;
    }

    public int getLoadBalancerPort() {
        return loadBalancerPort;
    }

    public void setLoadBalancerPort(int loadBalancerPort) {
        this.loadBalancerPort = loadBalancerPort;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getHealthCheckProtocol() {
        return healthCheckProtocol;
    }

    public void setHealthCheckProtocol(String healthCheckProtocol) {
        this.healthCheckProtocol = healthCheckProtocol;
    }

    public String getHealthCheckMethod() {
        return healthCheckMethod;
    }

    public void setHealthCheckMethod(String healthCheckMethod) {
        this.healthCheckMethod = healthCheckMethod;
    }

    public String getHealthCheckURI() {
        return healthCheckURI;
    }

    public void setHealthCheckURI(String healthCheckURI) {
        this.healthCheckURI = healthCheckURI;
    }

    public String getHealthCheckHttpCode() {
        return healthCheckHttpCode;
    }

    public void setHealthCheckHttpCode(String healthCheckHttpCode) {
        this.healthCheckHttpCode = healthCheckHttpCode;
    }

    public String getCertificateUuid() {
        return certificateUuid;
    }

    public void setCertificateUuid(String certificateUuid) {
        this.certificateUuid = certificateUuid;
    }

    public String getAclStatus() {
        return aclStatus;
    }

    public void setAclStatus(String aclStatus) {
        this.aclStatus = aclStatus;
    }

    public List<String> getAclUuids() {
        return aclUuids;
    }

    public void setAclUuids(List<String> aclUuids) {
        this.aclUuids = aclUuids;
    }

    public String getAclType() {
        return aclType;
    }

    public void setAclType(String aclType) {
        this.aclType = aclType;
    }

    public String getSecurityPolicyType() {
        return securityPolicyType;
    }

    public void setSecurityPolicyType(String securityPolicyType) {
        this.securityPolicyType = securityPolicyType;
    }

    public static APICreateLoadBalancerListenerMsg __example__() {
        APICreateLoadBalancerListenerMsg msg = new APICreateLoadBalancerListenerMsg();

        msg.setLoadBalancerUuid(uuid());
        msg.setName("Test-Listener");
        msg.setLoadBalancerPort(80);
        msg.setInstancePort(80);
        msg.setProtocol(LoadBalancerConstants.LB_PROTOCOL_HTTP);

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateLoadBalancerListenerEvent)rsp).getInventory().getUuid() : "", LoadBalancerListenerVO.class);
    }
}
