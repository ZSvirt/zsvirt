package org.zstack.sso.cas.service;

import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.identity.SessionInventory;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.sso.cas.header.CasUserToken;

/**
 * @Author: DaoDao
 * @Date: 2022/8/29
 */
public interface CasLogin {
    ErrorableValue<SessionInventory> createSessionWithToken(CasUserToken token);

    SessionInventory createSession(String accountUuid);

    ImportAccountSpec generateImportAccountSpec(CasUserToken token);
}
