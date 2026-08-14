package org.zstack.header.baremetal;

/**
 * Created by GuoYi on 2017/4/2.
 */
public interface BaremetalConstant {
    Integer WEBSOCKIFY_PORT = 6080;
    Integer NGINX_TERMINAL_PROXY_PORT = 7772;
    Integer SHELLINABOXD_PORT = 4200;
    String NGINX_CONF_PATH = "/etc/nginx/conf.d/mn_pxe/";

    String BAREMETAL_CLUSTER_TYPE = "baremetal";
    String BAREMETAL_HYPERVISOR_TYPE = "baremetal";

    Integer BM_CHASSIS_NUM_TRIAL_LICENSE = 1;
}
