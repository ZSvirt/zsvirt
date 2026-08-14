package org.zstack.header.baremetal.pxeserver;

/**
 * Created by GuoYi on 2017/3/25.
 *
 * Constants used by Bare Metal in ZStack.
 */
public interface BaremetalPxeServerConstant {
    String SERVICE_ID = "baremetal.pxeserver";
    String ACTION_CATEGORY = "baremetal.pxeserver";
    String SYNC_SIGNATURE_OF_BAREMETAL_PXESERVER = "sync-signature-of-baremetal-pxeserver-";

    String ECHO_PATH = "/baremetal/pxeserver/echo";
    String INIT_PATH = "/baremetal/pxeserver/init";
    String PING_PATH = "/baremetal/pxeserver/ping";
    String CONNECT_PATH = "/baremetal/pxeserver/connect";
    String START_PATH = "/baremetal/pxeserver/start";
    String STOP_PATH  = "/baremetal/pxeserver/stop";
    String CREATE_BM_CONFIGS_PATH = "/baremetal/pxeserver/createbmconfigs";
    String DELETE_BM_CONFIGS_PATH = "/baremetal/pxeserver/deletebmconfigs";
    String CREATE_BM_NGINX_PROXY_PATH = "/baremetal/pxeserver/createbmnginxproxy";
    String DELETE_BM_NGINX_PROXY_PATH = "/baremetal/pxeserver/deletebmnginxproxy";
    String CREATE_BM_NOVNC_PROXY_PATH = "/baremetal/pxeserver/createbmnovncproxy";
    String DELETE_BM_NOVNC_PROXY_PATH = "/baremetal/pxeserver/deletebmnovncproxy";
    String DOWNLOAD_FROM_CEPHB_PATH = "/baremetal/pxeserver/cephb/download";
    String DOWNLOAD_FROM_IMAGESTORE_PATH = "/baremetal/pxeserver/imagestore/download";
    String DELETE_BM_IMAGE_CACHE_PATH = "/baremetal/pxeserver/deletecache";
    String MOUNT_BM_IMAGE_CACHE_PATH = "/baremetal/pxeserver/mountcache";
    String CREATE_BM_DHCP_CONFIG_PATH = "/baremetal/pxeserver/createdhcpconfig";
    String DELETE_BM_DHCP_CONFIG_PATH = "/baremetal/pxeserver/deletedhcpconfig";

    String ANSIBLE_PLAYBOOK_NAME = "baremetalpxeserver.py";
    String ANSIBLE_MODULE_PATH = "ansible/baremetalpxeserver";

    String IPTABLES_COMMENTS = "pxeserver.allow.port";
}
