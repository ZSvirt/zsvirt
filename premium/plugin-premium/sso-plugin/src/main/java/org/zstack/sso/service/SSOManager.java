package org.zstack.sso.service;

import org.zstack.header.identity.SessionInventory;
import org.zstack.sso.oauth2.AuthCache;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * @Author: DaoDao
 * @Date: 2022/8/25
 */
public interface SSOManager {
    void redirect(HttpServletRequest request, HttpServletResponse response, SessionInventory session,
                  String clientUuid, AuthCache cache);
}
