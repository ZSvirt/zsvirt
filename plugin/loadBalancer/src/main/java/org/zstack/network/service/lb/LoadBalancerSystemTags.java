package org.zstack.network.service.lb;

import org.zstack.header.tag.TagDefinition;
import org.zstack.tag.PatternedSystemTag;

/**
 * Created by frank on 8/9/2015.
 */
@TagDefinition
public class LoadBalancerSystemTags {
    public static PatternedSystemTag SEPARATE_VR = new PatternedSystemTag("separateVirtualRouterVm", LoadBalancerVO.class);

    public static final String HEALTHY_THRESHOLD_TOKEN = "healthyThreshold";
    public static PatternedSystemTag HEALTHY_THRESHOLD = new PatternedSystemTag(String.format("healthyThreshold::{%s}", HEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_INTERVAL_TOKEN = "healthCheckInterval";
    public static PatternedSystemTag HEALTH_INTERVAL = new PatternedSystemTag(String.format("healthCheckInterval::{%s}", HEALTH_INTERVAL_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TARGET_TOKEN = "healthCheckTarget";
    public static PatternedSystemTag HEALTH_TARGET = new PatternedSystemTag(String.format("healthCheckTarget::{%s}", HEALTH_TARGET_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String HEALTH_TIMEOUT_TOKEN = "healthCheckTimeout";
    public static PatternedSystemTag HEALTH_TIMEOUT = new PatternedSystemTag(String.format("healthCheckTimeout::{%s}", HEALTH_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String UNHEALTHY_THRESHOLD_TOKEN = "unhealthyThreshold";
    public static PatternedSystemTag UNHEALTHY_THRESHOLD = new PatternedSystemTag(String.format("unhealthyThreshold::{%s}", UNHEALTHY_THRESHOLD_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String CONNECTION_IDLE_TIMEOUT_TOKEN = "connectionIdleTimeout";
    public static PatternedSystemTag CONNECTION_IDLE_TIMEOUT = new PatternedSystemTag(String.format("connectionIdleTimeout::{%s}", CONNECTION_IDLE_TIMEOUT_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String MAX_CONNECTION_TOKEN = "maxConnection";
    public static PatternedSystemTag MAX_CONNECTION = new PatternedSystemTag(String.format("maxConnection::{%s}", MAX_CONNECTION_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String NUMBER_OF_PROCESS_TOKEN = "Nbprocess";
    public static PatternedSystemTag NUMBER_OF_PROCESS = new PatternedSystemTag(String.format("Nbprocess::{%s}", NUMBER_OF_PROCESS_TOKEN),
            LoadBalancerListenerVO.class);

    public static final String BALANCER_ALGORITHM_TOKEN = "balancerAlgorithm";
    public static PatternedSystemTag BALANCER_ALGORITHM = new PatternedSystemTag(String.format("balancerAlgorithm::{%s}", BALANCER_ALGORITHM_TOKEN), LoadBalancerListenerVO.class);

    public static final String SESSION_PERSISTENCE_TOKEN = "sessionPersistence";
    public static PatternedSystemTag SESSION_PERSISTENCE = new PatternedSystemTag(String.format("sessionPersistence::{%s}", SESSION_PERSISTENCE_TOKEN), LoadBalancerListenerVO.class);

    public static final String SESSION_IDLE_TIMEOUT_TOKEN = "sessionIdleTimeout";
    public static PatternedSystemTag SESSION_IDLE_TIMEOUT = new PatternedSystemTag(String.format("sessionIdleTimeout::{%s}", SESSION_IDLE_TIMEOUT_TOKEN), LoadBalancerListenerVO.class);

    public static final String COOKIE_NAME_TOKEN = "cookieName";
    public static PatternedSystemTag COOKIE_NAME = new PatternedSystemTag(String.format("cookieName::{%s}", COOKIE_NAME_TOKEN), LoadBalancerListenerVO.class);

    public static final String HTTP_REDIRECT_HTTPS_TOKEN = "httpRedirectHttps ";
    public static PatternedSystemTag HTTP_REDIRECT_HTTPS = new PatternedSystemTag(String.format("httpRedirectHttps::{%s}", HTTP_REDIRECT_HTTPS_TOKEN), LoadBalancerListenerVO.class);

    public static final String REDIRECT_PORT_TOKEN = "redirectPort";
    public static PatternedSystemTag REDIRECT_PORT = new PatternedSystemTag(String.format("redirectPort::{%s}", REDIRECT_PORT_TOKEN), LoadBalancerListenerVO.class);

    public static final String STATUS_CODE_TOKEN = "statusCode";
    public static PatternedSystemTag STATUS_CODE = new PatternedSystemTag(String.format("statusCode::{%s}", STATUS_CODE_TOKEN), LoadBalancerListenerVO.class);

    public static final String BALANCER_WEIGHT_TOKEN = "balancerWeight";
    public static final String BALANCER_NIC_TOKEN = "balancerNic";
    public static PatternedSystemTag BALANCER_WEIGHT = new PatternedSystemTag(String.format("balancerWeight::{%s}::{%s}", BALANCER_NIC_TOKEN, BALANCER_WEIGHT_TOKEN), LoadBalancerListenerVO.class);

    public static final String HEALTH_PARAMETER_TOKEN = "healthCheckParameter";
    public static PatternedSystemTag HEALTH_PARAMETER = new PatternedSystemTag(String.format("healthCheckParameter::{%s}", HEALTH_PARAMETER_TOKEN), LoadBalancerListenerVO.class);

    public static final String BALANCER_ACL_TOKEN = "accessControlStatus";
    public static PatternedSystemTag BALANCER_ACL = new PatternedSystemTag(String.format("accessControlStatus::{%s}", BALANCER_ACL_TOKEN), LoadBalancerListenerVO.class);

    public static final String HTTP_MODE_TOKEN = "httpMode";
    public static PatternedSystemTag HTTP_MODE= new PatternedSystemTag(String.format("httpMode::{%s}", HTTP_MODE_TOKEN), LoadBalancerListenerVO.class);

}
