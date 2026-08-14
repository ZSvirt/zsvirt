package org.zstack.storage.zbs;

import org.springframework.web.util.UriComponentsBuilder;

/**
 * @author Xingwei Yu
 * @date 2024/3/27 17:39
 */
public class ZbsAgentUrl {
    public static String primaryStorageUrl(String ip, String path) {
        UriComponentsBuilder ub = UriComponentsBuilder.newInstance();
        ub.scheme("http");
        ub.host(ip);
        ub.port(ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_PORT);
        if (!"".equals(ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH)) {
            ub.path(ZbsGlobalProperty.PRIMARY_STORAGE_AGENT_URL_ROOT_PATH);
        }
        ub.path(path);
        return ub.build().toUriString();
    }
}
