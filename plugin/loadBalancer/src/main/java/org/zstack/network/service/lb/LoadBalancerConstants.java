package org.zstack.network.service.lb;

import org.zstack.header.network.service.NetworkServiceType;
import org.zstack.header.vm.VmInstanceConstant;

import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;

/**
 * Created by frank on 8/8/2015.
 */
public class LoadBalancerConstants {
    public static final String SERVICE_ID = "loadBalancer";

    public static final String LB_NETWORK_SERVICE_TYPE_STRING = "LoadBalancer";

    public static final NetworkServiceType LB_NETWORK_SERVICE_TYPE = new NetworkServiceType(LB_NETWORK_SERVICE_TYPE_STRING);

    public static final String BALANCE_ALGORITHM_ROUND_ROBIN = "roundrobin";
    public static final String BALANCE_ALGORITHM_LEAST_CONN = "leastconn";
    public static final String BALANCE_ALGORITHM_LEAST_SOURCE = "source";
    public static final String BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN = "weightroundrobin";

    public static final List<String> BALANCE_ALGORITHMS = new ArrayList<String>();

    public static final String HEALTH_CHECK_TARGET_PROTOCL_TCP = "tcp";
    public static final String HEALTH_CHECK_TARGET_PROTOCL_UDP = "udp";
    public static final String HEALTH_CHECK_TARGET_PROTOCL_HTTP = "http";

    public static final String HTTP_MODE_HTTP_KEEP_ALIVE = "http-keep-alive";
    public static final String HTTP_MODE_HTTP_SERVER_CLOSE = "http-server-close";
    public static final String HTTP_MODE_HTTP_TUNNEL = "http-tunnel";
    public static final String HTTP_MODE_HTTPCLOSE = "httpclose";
    public static final String HTTP_MODE_FORCECLOSE = "forceclose";

    public static enum HealthCheckMothod {
        GET,
        HEAD
    }

    public static enum MatchMethod {
        Domain,
        Url,
        DomainAndUrl
    }

    public static enum HealthCheckStatusCode {
        http_2xx,
        http_3xx,
        http_4xx,
        http_5xx
    }

    public static enum HttpRedirectHttps {
        enable,
        disable
    }

    public static final String HEALTH_CHECK_URI_REGEX = "^/[A-Za-z0-9-/.%?#&]*";

    public static final String COOKIE_NAME_REGEX = "[A-Za-z0-9_-]+";

    public static final List<String> HEALTH_CHECK_TARGET_PROTOCOLS = new ArrayList<String>();

    public static final List<String> HTTP_MODES = new ArrayList<String>();

    public static final String ACTION_CATEGORY = "loadBalancer";

    public static final String LB_PROTOCOL_UDP = "udp";
    public static final String LB_PROTOCOL_TCP = "tcp";
    public static final String LB_PROTOCOL_HTTP = "http";
    public static final String LB_PROTOCOL_HTTPS = "https";
    public static final int PROTOCOL_HTTP_DEFAULT_PORT = 80;
    public static final int PROTOCOL_HTTPS_DEFAULT_PORT = 443;

    public static final int DNS_PORT = 53;
    public static final int SSH_PORT = 22;
    public static final int ZVR_PORT = 7272;

    /*max concurrent connect no more than MAX_CONNECTION_LIMIT per listener*/
    public static final long MAX_CONNECTION_LIMIT = 10000000;
    public static final long BALANCER_WEIGHT_MAX = 100;
    public static final long BALANCER_WEIGHT_MIN = 0;
    public static final long BALANCER_WEIGHT_default = 100;
    static {
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_ROUND_ROBIN);
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_LEAST_CONN);
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_LEAST_SOURCE);
        BALANCE_ALGORITHMS.add(BALANCE_ALGORITHM_WEIGHT_ROUND_ROBIN);

        HEALTH_CHECK_TARGET_PROTOCOLS.add(HEALTH_CHECK_TARGET_PROTOCL_TCP);
        HEALTH_CHECK_TARGET_PROTOCOLS.add(HEALTH_CHECK_TARGET_PROTOCL_UDP);
        HEALTH_CHECK_TARGET_PROTOCOLS.add(HEALTH_CHECK_TARGET_PROTOCL_HTTP);

        HTTP_MODES.add(HTTP_MODE_HTTP_KEEP_ALIVE);
        HTTP_MODES.add(HTTP_MODE_HTTP_SERVER_CLOSE);
        HTTP_MODES.add(HTTP_MODE_HTTP_TUNNEL);
        HTTP_MODES.add(HTTP_MODE_HTTPCLOSE);
        HTTP_MODES.add(HTTP_MODE_FORCECLOSE);
    }

    public static enum Param {
        LOAD_BALANCER_VO,
        LOAD_BALANCER_LISTENER_LOAD_BALANCER_SERVERGROUP,
    }

    public static final int CONNECTION_IDLE_TIMEOUT_MIN = 0;
    public static final int CONNECTION_IDLE_TIMEOUT_MAX = Integer.MAX_VALUE;

    public static final int MAXIMUM_CONNECTION_MIN = 0;
    public static final int MAXIMUM_CONNECTION_MAX = (int)MAX_CONNECTION_LIMIT;

    public static final int HEALTH_CHECK_THRESHOLD_MIN = 1;
    public static final int HEALTH_CHECK_THRESHOLD_MAX = Integer.MAX_VALUE;

    public static final int UNHEALTH_CHECK_THRESHOLD_MIN = 1;
    public static final int UNHEALTH_CHECK_THRESHOLD_MAX = Integer.MAX_VALUE;

    public static final int HEALTH_CHECK_INTERVAL_MIN = 1;
    public static final int HEALTH_CHECK_INTERVAL_MAX = Integer.MAX_VALUE;

    public static final int NUMBER_OF_PROCESS_MIN = 1;
    public static final int NUMBER_OF_PROCESS_MAX = 64;

    public static final int SESSION_IDLE_TIMEOUT_MIN = 30;
    public static final int SESSION_IDLE_TIMEOUT_MAX = 3600;
    public static final int SESSION_IDLE_TIMEOUT_DEFAULT = 60;

    public static final int COOKIE_NAME_MAX = 20;

    public static final int REDIRECT_PORT_DEFAULT = 443;
    public static final int STATUS_CODE_DEFAULT = 302;

    public static final String HEALTH_CHECK_TARGET_DEFAULT = "default";

    public static final List<VmInstanceConstant.VmOperation> vmOperationForDetachListener = asList(
            VmInstanceConstant.VmOperation.Destroy,
            VmInstanceConstant.VmOperation.DetachNic,
            VmInstanceConstant.VmOperation.ChangeNicNetwork,
            VmInstanceConstant.VmOperation.ChangeNicIp
    );
}
