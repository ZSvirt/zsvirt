package org.zstack.sso.service.redirect;

import java.util.Map;

/**
 * @Author: DaoDao
 * @Date: 2022/9/6
 */
public interface UrlTemplate {
    String getRedirectUrl(Map<String, String> params, String templateUuid);
}
