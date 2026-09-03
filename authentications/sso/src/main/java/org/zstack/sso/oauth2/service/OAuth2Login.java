package org.zstack.sso.oauth2.service;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.sso.oauth2.header.OAuth2UserToken;

/**
 * @Author: DaoDao
 * @Date: 2022/8/25
 */
public interface OAuth2Login {
    ErrorableValue<SessionInventory> createSessionWithToken(OAuth2UserToken token);

    SessionInventory createSession(String accountUuid);

    ImportAccountSpec generateImportAccountSpec(OAuth2UserToken token);
}

