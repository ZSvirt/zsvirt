package org.zstack.network.service.lb;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.SystemTag;

/**
 * Created by frank on 8/9/2015.
 */
@TagDefinition
public class LoadBalancerSystemTags {
    public static SystemTag SEPARATE_VR = new SystemTag("separateVirtualRouterVm", LoadBalancerVO.class);

    public static final String HEALTHY_THRESHOLD_TOKEN = "healthyThreshold";
    public static SystemTag HEALTHY_THRESHOLD = new SystemTag(String.format("healthyThreshold::{%s}", HEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_INTERVAL_TOKEN = "healthCheckInterval";
    public static SystemTag HEALTH_INTERVAL = new SystemTag(String.format("healthCheckInterval::{%s}", HEALTH_INTERVAL_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TARGET_TOKEN = "healthCheckTarget";
    public static SystemTag HEALTH_TARGET = new SystemTag(String.format("healthCheckTarget::{%s}", HEALTH_TARGET_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TIMEOUT_TOKEN = "healthCheckTimeout";
    public static SystemTag HEALTH_TIMEOUT = new SystemTag(String.format("healthCheckTimeout::{%s}", HEALTH_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String UNHEALTHY_THRESHOLD_TOKEN = "unhealthyThreshold";
    public static SystemTag UNHEALTHY_THRESHOLD = new SystemTag(String.format("unhealthyThreshold::{%s}", UNHEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String CONNECTION_IDLE_TIMEOUT_TOKEN = "connectionIdleTimeout";
    public static SystemTag CONNECTION_IDLE_TIMEOUT = new SystemTag(String.format("connectionIdleTimeout::{%s}", CONNECTION_IDLE_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String MAX_CONNECTION_TOKEN = "maxConnection";
    public static SystemTag MAX_CONNECTION = new SystemTag(String.format("maxConnection::{%s}", MAX_CONNECTION_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String NUMBER_OF_PROCESS_TOKEN = "Nbprocess";
    public static SystemTag NUMBER_OF_PROCESS = new SystemTag(String.format("Nbprocess::{%s}", NUMBER_OF_PROCESS_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String BALANCER_ALGORITHM_TOKEN = "balancerAlgorithm";
    public static SystemTag BALANCER_ALGORITHM = new SystemTag(String.format("balancerAlgorithm::{%s}", BALANCER_ALGORITHM_TOKEN), LoadBalancerListenerVO.class);

    public static final String SESSION_PERSISTENCE_TOKEN = "sessionPersistence";
    public static SystemTag SESSION_PERSISTENCE = new SystemTag(String.format("sessionPersistence::{%s}", SESSION_PERSISTENCE_TOKEN), LoadBalancerListenerVO.class);

    public static final String SESSION_IDLE_TIMEOUT_TOKEN = "sessionIdleTimeout";
    public static SystemTag SESSION_IDLE_TIMEOUT = new SystemTag(String.format("sessionIdleTimeout::{%s}", SESSION_IDLE_TIMEOUT_TOKEN), LoadBalancerListenerVO.class);

    public static final String COOKIE_NAME_TOKEN = "cookieName";
    public static SystemTag COOKIE_NAME = new SystemTag(String.format("cookieName::{%s}", COOKIE_NAME_TOKEN), LoadBalancerListenerVO.class);

    public static final String HTTP_REDIRECT_HTTPS_TOKEN = "httpRedirectHttps ";
    public static SystemTag HTTP_REDIRECT_HTTPS = new SystemTag(String.format("httpRedirectHttps::{%s}", HTTP_REDIRECT_HTTPS_TOKEN), LoadBalancerListenerVO.class);

    public static final String REDIRECT_PORT_TOKEN = "redirectPort";
    public static SystemTag REDIRECT_PORT = new SystemTag(String.format("redirectPort::{%s}", REDIRECT_PORT_TOKEN), LoadBalancerListenerVO.class);

    public static final String STATUS_CODE_TOKEN = "statusCode";
    public static SystemTag STATUS_CODE = new SystemTag(String.format("statusCode::{%s}", STATUS_CODE_TOKEN), LoadBalancerListenerVO.class);

    public static final String BALANCER_WEIGHT_TOKEN = "balancerWeight";
    public static final String BALANCER_NIC_TOKEN = "balancerNic";
    public static SystemTag BALANCER_WEIGHT = new SystemTag(String.format("balancerWeight::{%s}::{%s}", BALANCER_NIC_TOKEN, BALANCER_WEIGHT_TOKEN), LoadBalancerListenerVO.class);

    public static final String HEALTH_PARAMETER_TOKEN = "healthCheckParameter";
    public static SystemTag HEALTH_PARAMETER = new SystemTag(String.format("healthCheckParameter::{%s}", HEALTH_PARAMETER_TOKEN), LoadBalancerListenerVO.class);

    public static final String BALANCER_ACL_TOKEN = "accessControlStatus";
    public static SystemTag BALANCER_ACL = new SystemTag(String.format("accessControlStatus::{%s}", BALANCER_ACL_TOKEN), LoadBalancerListenerVO.class);

    public static final String HTTP_MODE_TOKEN = "httpMode";
    public static SystemTag HTTP_MODE= new SystemTag(String.format("httpMode::{%s}", HTTP_MODE_TOKEN), LoadBalancerListenerVO.class);

}
