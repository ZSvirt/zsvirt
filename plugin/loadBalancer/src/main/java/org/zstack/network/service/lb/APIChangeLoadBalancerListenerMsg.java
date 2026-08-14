package org.zstack.network.service.lb;

import org.springframework.http.HttpMethod;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;

/**
 * Created by shixin.ruan on 02/25/2019.
 */
@RestRequest(
        path = "/load-balancers/listeners/{uuid}/actions",
        method = HttpMethod.PUT,
        responseClass = APIChangeLoadBalancerListenerEvent.class,
        isAction = true
)
public class APIChangeLoadBalancerListenerMsg extends APIMessage implements LoadBalancerListenerMsg , LoadBalancerMessage {
    @APIParam(resourceType = LoadBalancerListenerVO.class)
    private String uuid;

    @APIParam(numberRange = {LoadBalancerConstants.CONNECTION_IDLE_TIMEOUT_MIN, LoadBalancerConstants.CONNECTION_IDLE_TIMEOUT_MAX}, required = false)
    private Integer connectionIdleTimeout;

    @APIParam(numberRange = {LoadBalancerConstants.MAXIMUM_CONNECTION_MIN, LoadBalancerConstants.MAXIMUM_CONNECTION_MAX}, required = false)
    private Integer maxConnection;

    @APIParam(validValues = {LoadBalancerConstants.BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN, LoadBalancerConstants.BALANCE_ALGORITHM_ROUND_ROBIN, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_CONN, LoadBalancerConstants.BALANCE_ALGORITHM_LEAST_SOURCE}, required = false)
    private String balancerAlgorithm;

    @APIParam(required = false)
    private String healthCheckTarget;

    @APIParam(numberRange = {LoadBalancerConstants.HEALTH_CHECK_THRESHOLD_MIN, LoadBalancerConstants.HEALTH_CHECK_THRESHOLD_MAX}, required = false)
    private Integer healthyThreshold;

    @APIParam(numberRange = {LoadBalancerConstants.UNHEALTH_CHECK_THRESHOLD_MIN, LoadBalancerConstants.UNHEALTH_CHECK_THRESHOLD_MAX}, required = false)
    private Integer unhealthyThreshold;

    @APIParam(numberRange = {LoadBalancerConstants.HEALTH_CHECK_INTERVAL_MIN, LoadBalancerConstants.HEALTH_CHECK_INTERVAL_MAX}, required = false)
    private Integer healthCheckInterval;

    @APIParam(validValues = {LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_TCP, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_UDP, LoadBalancerConstants.HEALTH_CHECK_TARGET_PROTOCL_HTTP}, required = false)
    private String healthCheckProtocol;
    @APIParam(validValues = {"GET", "HEAD"}, required = false)
    private String healthCheckMethod;
    @APIParam(validRegexValues = LoadBalancerConstants.HEALTH_CHECK_URI_REGEX, maxLength = 80, required = false)
    private String healthCheckURI;
    @APIParam(maxLength = 80, required = false)
    private String healthCheckHttpCode;
    @APIParam(validValues = {"enable", "disable"}, required = false)
    private String aclStatus;
    @APIParam(validValues = {LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_DEFAULT, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_0, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_1,
            LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2, LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2_STRICT,
            LoadBalanceSecurityPolicyConstant.TLS_CIPHER_POLICY_1_2_STRICT_WITH_1_3}, required = false)
    private String securityPolicyType;

    @APIParam(numberRange = {LoadBalancerConstants.NUMBER_OF_PROCESS_MIN, LoadBalancerConstants.NUMBER_OF_PROCESS_MAX}, required = false)
    private Integer nbprocess;

    @APIParam(validValues = {"http-keep-alive", "http-server-close", "http-tunnel", "httpclose", "forceclose"}, required = false)
    private String httpMode;

    @APIParam(validValues = {"disable", "iphash", "insert", "rewrite"}, required = false)
    private String sessionPersistence;

    @APIParam(numberRange = {LoadBalancerConstants.SESSION_IDLE_TIMEOUT_MIN, LoadBalancerConstants.SESSION_IDLE_TIMEOUT_MAX}, required = false)
    private Integer sessionIdleTimeout;

    @APIParam(validRegexValues = LoadBalancerConstants.COOKIE_NAME_REGEX, maxLength = LoadBalancerConstants.COOKIE_NAME_MAX, required = false)
    private String cookieName;

    @APIParam(validValues = {"disable", "enable"}, required = false)
    private String httpRedirectHttps;

    @APIParam(numberRange = {1, 65535}, required = false)
    private Integer redirectPort;

    @APIParam(validValues = {"301", "302", "303", "307", "308"}, required = false)
    private Integer statusCode;

    @APINoSee
    private String loadBalancerUuid;

    public String getAclStatus() {
        return aclStatus;
    }

    public void setAclStatus(String aclStatus) {
        this.aclStatus = aclStatus;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public Integer getConnectionIdleTimeout() {
        return connectionIdleTimeout;
    }

    public void setConnectionIdleTimeout(Integer connectionIdleTimeout) {
        this.connectionIdleTimeout = connectionIdleTimeout;
    }

    public Integer getMaxConnection() {
        return maxConnection;
    }

    public void setMaxConnection(Integer maxConnection) {
        this.maxConnection = maxConnection;
    }

    public String getBalancerAlgorithm() {
        return balancerAlgorithm;
    }

    public void setBalancerAlgorithm(String balancerAlgorithm) {
        this.balancerAlgorithm = balancerAlgorithm;
    }

    public String getHealthCheckTarget() {
        return healthCheckTarget;
    }

    public void setHealthCheckTarget(String healthCheckTarget) {
        this.healthCheckTarget = healthCheckTarget;
    }

    public Integer getHealthyThreshold() {
        return healthyThreshold;
    }

    public void setHealthyThreshold(Integer healthyThreshold) {
        this.healthyThreshold = healthyThreshold;
    }

    public Integer getUnhealthyThreshold() {
        return unhealthyThreshold;
    }

    public void setUnhealthyThreshold(Integer unhealthyThreshold) {
        this.unhealthyThreshold = unhealthyThreshold;
    }

    public Integer getHealthCheckInterval() {
        return healthCheckInterval;
    }

    public void setHealthCheckInterval(Integer healthCheckInterval) {
        this.healthCheckInterval = healthCheckInterval;
    }

    public String getLoadBalancerUuid() {
        return loadBalancerUuid;
    }

    public void setLoadBalancerUuid(String loadBalancerUuid) {
        this.loadBalancerUuid = loadBalancerUuid;
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

    public String getSecurityPolicyType() {
        return securityPolicyType;
    }

    public void setSecurityPolicyType(String securityPolicyType) {
        this.securityPolicyType = securityPolicyType;
    }

    public String getSessionPersistence() {
        return sessionPersistence;
    }

    public void setSessionPersistence(String sessionPersistence) {
        this.sessionPersistence = sessionPersistence;
    }

    public Integer getSessionIdleTimeout() {
        return sessionIdleTimeout;
    }

    public void setSessionIdleTimeout(Integer sessionIdleTimeout) {
        this.sessionIdleTimeout = sessionIdleTimeout;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getHttpRedirectHttps() {
        return httpRedirectHttps;
    }

    public void setHttpRedirectHttps(String httpRedirectHttps) {
        this.httpRedirectHttps = httpRedirectHttps;
    }

    public Integer getRedirectPort() {
        return redirectPort;
    }

    public void setRedirectPort(Integer redirectPort) {
        this.redirectPort = redirectPort;
    }

    public Integer getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(Integer statusCode) {
        this.statusCode = statusCode;
    }

    @Override
    public String getLoadBalancerListenerUuid() {
        return uuid;
    }

    public Integer getNbprocess() {
        return nbprocess;
    }

    public void setNbprocess(Integer nbprocess) {
        this.nbprocess = nbprocess;
    }

    public String getHttpMode() {
        return httpMode;
    }

    public void setHttpMode(String httpMode) {
        this.httpMode = httpMode;
    }

    public static APIChangeLoadBalancerListenerMsg __example__() {
        APIChangeLoadBalancerListenerMsg msg = new APIChangeLoadBalancerListenerMsg();

        msg.setUuid(uuid());
        msg.setBalancerAlgorithm("roundrobin");
        msg.setSessionPersistence(LoadBalancerSessionPersistence.insert.toString());
        msg.setSessionIdleTimeout(60);
        msg.setConnectionIdleTimeout(300);
        msg.setHealthCheckInterval(5);
        msg.setHealthCheckTarget("default");
        msg.setHealthyThreshold(2);
        msg.setMaxConnection(5000);
        msg.setUnhealthyThreshold(3);
        msg.setNbprocess(1);
        msg.setHttpMode("http-keep-alive");

        return msg;
    }
}
