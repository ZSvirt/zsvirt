package org.zstack.sso.oauth2.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.cloudbus.EventFacadeImpl;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorableValue;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.LoginSessionInfo;
import org.zstack.identity.AccountManager;
import org.zstack.identity.Session;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO;
import org.zstack.identity.imports.entity.AccountThirdPartyAccountSourceRefVO_;
import org.zstack.identity.imports.entity.SyncCreatedAccountStrategy;
import org.zstack.identity.imports.header.ImportAccountItem;
import org.zstack.identity.imports.header.ImportAccountSpec;
import org.zstack.sso.oauth2.header.OAuth2UserToken;
import org.zstack.sso.service.SSOManager;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.List;

import static org.zstack.core.Platform.err;
import static org.zstack.core.Platform.operr;
import static org.zstack.sso.SSOConstants.OAUTH2_CLIENT_TYPE;
import static org.zstack.sso.header.SSOErrors.*;
import static org.zstack.utils.CollectionDSL.*;

/**
 * @Author: DaoDao
 * @Date: 2022/8/25
 */
public class OAuth2LoginIAM1 implements OAuth2Login {
    private static final CLogger logger = Utils.getLogger(OAuth2LoginIAM1.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private SSOManager sso;
    @Autowired
    private AccountManager accountManager;
    @Autowired
    private EventFacadeImpl eventFacade;

    @Override
    public ErrorableValue<SessionInventory> createSessionWithToken(OAuth2UserToken token) {
        List<String> accountUuidList = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.credentials, token.sub)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, token.clientUuid)
                .select(AccountThirdPartyAccountSourceRefVO_.accountUuid)
                .listValues();

        if (accountUuidList.size() == 1) {
            String accountUuid = accountUuidList.get(0);
            // ZSV-10785: the 3rd users may be blocked in ZSphere as well,
            // thus, here must double-check the status of the user account.
            // if the status of the user account is disabled or blocked,
            // then the 3rd users cannot login our platform.
            ErrorCode ec = checkIfAccountIsBlocked(accountUuid);
            if (ec != null) {
                return ErrorableValue.ofErrorCode(ec);
            }
            return ErrorableValue.of(createSession(accountUuid));
        } else if (accountUuidList.size() > 1) {
            return ErrorableValue.ofErrorCode(
                    operr("multiple accounts found for OAuth2 user[sub=%s]", token.sub));
        }

        // for compatibility
        // Account uuid list is empty, maybe first login (after MN updated to 4.10.6+)
        // Before version 4.10.6, AccountThirdPartyAccountSourceRefVO.credentials for OAuth2 user is "::${accountUuid}"
        //     (has prefix with "::")
        // Since version 4.10.6, AccountThirdPartyAccountSourceRefVO.credentials is OAuth2.sid
        accountUuidList = Q.New(AccountThirdPartyAccountSourceRefVO.class)
                .like(AccountThirdPartyAccountSourceRefVO_.credentials, "::%")
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, token.clientUuid)
                .select(AccountThirdPartyAccountSourceRefVO_.accountUuid)
                .listValues();
        if (accountUuidList.isEmpty()) {
            return ErrorableValue.ofErrorCode(err(SSO_ACCOUNT_NOT_FOUND,
                    "failed to find account for OAuth2 user[name=%s], maybe logging in for the first time",
                    token.username));
        }

        List<String> accountList = Q.New(AccountVO.class)
                .in(AccountVO_.uuid, accountUuidList)
                .eq(AccountVO_.type, AccountType.ThirdParty)
                .eq(AccountVO_.name, token.username)
                .select(AccountVO_.uuid)
                .listValues();
        if (accountList.isEmpty()) {
            return ErrorableValue.ofErrorCode(err(SSO_ACCOUNT_NOT_FOUND,
                    "failed to find account for OAuth2 user[name=%s], maybe logging in for the first time",
                    token.username));
        }
        if (accountList.size() > 1) {
            return ErrorableValue.ofErrorCode(
                    operr("multiple accounts found for OAuth2 user[AccountVO.username=%s]", token.username));
        }

        String accountUuid = accountList.get(0);
        SQL.New(AccountThirdPartyAccountSourceRefVO.class)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountUuid, accountUuid)
                .eq(AccountThirdPartyAccountSourceRefVO_.accountSourceUuid, token.clientUuid)
                .set(AccountThirdPartyAccountSourceRefVO_.credentials, token.sub)
                .update();
        return ErrorableValue.of(createSession(accountUuid));
    }

    private ErrorCode checkIfAccountIsBlocked(String accountUuid) {
        List<Tuple> accountTuples = Q.New(AccountVO.class)
                .eq(AccountVO_.uuid, accountUuid)
                .select(AccountVO_.uuid, AccountVO_.state)
                .listTuple();
        if (accountTuples.size() == 1) {
            Tuple tuple = accountTuples.get(0);
            AccountState state = tuple.get(1, AccountState.class);
            if (state == AccountState.Disabled) {
                return err(IdentityErrors.ACCOUNT_DISABLED, "failed to login: account is disabled");
            } else if (state == AccountState.Staled) {
                return err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password");
            }
        }
        return null;
    }

    @Override
    public SessionInventory createSession(String accountUuid) {
        SessionInventory session = Session.login(accountUuid);
        IdentityCanonicalEvents.AccountLoginData data = new IdentityCanonicalEvents.AccountLoginData();
        data.setAccountUuid(accountUuid);
        eventFacade.fire(IdentityCanonicalEvents.ACCOUNT_LOGIN_PATH, data);
        return session;
    }

    @Override
    public ImportAccountSpec generateImportAccountSpec(OAuth2UserToken token) {
        ImportAccountSpec spec = new ImportAccountSpec();
        spec.setSourceUuid(token.clientUuid);
        spec.setSourceType(OAUTH2_CLIENT_TYPE);
        spec.setSyncCreateStrategy(SyncCreatedAccountStrategy.CreateAccount);

        ImportAccountItem item = new ImportAccountItem();
        item.setAccountUuid(Platform.getUuid());
        item.setAccountType(AccountType.ThirdParty);
        item.setCredentials(token.sub);
        item.setUsername(token.username);
        spec.setAccountList(list(item));

        return spec;
    }
}

