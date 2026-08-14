package org.zstack.storage.ceph;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * Created by xing5 on 2017/9/22.
 */
public class CephAgentUrl {
    public static String primaryStorageUrl(String ip, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        ub.host(ip);
        ub.port(CephGlobalProperty.PRIMARY_STORAGE_AGENT_PORT);
        if (!"".equals(CephGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH)) {
            ub.path(CephGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH);
        }
        ub.path(path);
        return ub.build().toUriString();
    }

    public static String backupStorageUrl(String ip, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        ub.host(ip);
        ub.port(CephGlobalProperty.BACKUP_STORAGE_AGENT_PORT);
        if (!"".equals(CephGlobalProperty.BACKUP_STORAGE_AGENT_URL_ROOT_PATH)) {
            ub.path(CephGlobalProperty.BACKUP_STORAGE_AGENT_URL_ROOT_PATH);
        }
        ub.path(path);
        return ub.build().toUriString();
    }
}
