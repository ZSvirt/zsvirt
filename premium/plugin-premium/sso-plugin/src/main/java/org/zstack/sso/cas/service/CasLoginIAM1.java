package org.zstack.sso.cas.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.Q;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.identity.*;
import org.zstack.identity.Session;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.header.ImportAccountItem;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.sso.cas.header.CasUserToken;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.sso.SSOConstants.CAS_CLIENT_TYPE;
import static org.zstack.sso.header.SSOErrors.*;
import static org.zstack.utils.CollectionDSL.*;

/**
 * @Author: DaoDao
 * @Date: 2022/8/29
 */
public class CasLoginIAM1 implements CasLogin {
    private static final CLogger logger = Utils.getLogger(CasLoginIAM1.class);
    @Autowired
    private EventFacade evtf;

    @Override
    public ErrorableValue<SessionInventory> createSessionWithToken(CasUserToken token) {
        List<String> accountUuidList = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.credentials, token.username)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, token.clientUuid)
                .select(AccountThirdPartyAccountSourceRefVO_.accountUuid)
                .listValues();

        if (accountUuidList.isEmpty()) {
            return ErrorableValue.ofErrorCode(err(SSO_ACCOUNT_NOT_FOUND,
                    "failed to find account for CAS user[name=%s]", token.username));
        }
        if (accountUuidList.size() > 1) {
            return ErrorableValue.ofErrorCode(
                    operr("multiple accounts found for CAS user[name=%s]", token.username));
        }

        return ErrorableValue.of(createSession(accountUuidList.get(0)));
    }

    @Override
    public SessionInventory createSession(String accountUuid) {
        SessionInventory session = Session.login(accountUuid);
        IdentityCanonicalEvents.AccountLoginData data = new IdentityCanonicalEvents.AccountLoginData();
        data.setAccountUuid(accountUuid);
        evtf.fire(IdentityCanonicalEvents.ACCOUNT_LOGIN_PATH, data);
        return session;
    }

    @Override
    public ImportAccountSpec generateImportAccountSpec(CasUserToken token) {
        ImportAccountSpec spec = new ImportAccountSpec();
        spec.setSourceUuid(token.clientUuid);
        spec.setSourceType(CAS_CLIENT_TYPE);
        spec.setSyncCreateStrategy(SyncCreatedAccountStrategy.CreateAccount);

        ImportAccountItem item = new ImportAccountItem();
        item.setAccountUuid(Platform.getUuid());
        item.setAccountType(AccountType.ThirdParty);
        item.setCredentials(token.username);
        item.setUsername(token.username);
        spec.setAccountList(list(item));

        return spec;
    }
}
