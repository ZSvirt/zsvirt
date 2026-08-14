package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.Q;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.identity.*;
import org.zstack.header.identity.login.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.core.Platform.err;

/**
 * Created by kayo on 2018/7/10.
 */
public class AccountLoginBackend implements LoginBackend {
    private static final CLogger logger = Utils.getLogger(AccountLoginBackend.class);

    public static final LoginType loginType = new LoginType(AccountConstant.LOGIN_TYPE);

    @Autowired
    private PluginRegistry pluginRgty;

    @Override
    public LoginType getLoginType() {
        return loginType;
    }

    @Override
    public void login(LoginContext loginContext, ReturnValueCompletion<LoginSessionInfo> completion) {
        List<Tuple> accountTuples = Q.New(AccountVO.class)
                .eq(AccountVO_.name, loginContext.getUsername())
                .eq(AccountVO_.password, loginContext.getPassword())
                .select(AccountVO_.uuid, AccountVO_.state)
                .listTuple();

        String accountType = loginContext.getProperties().get(AccountConstant.ACCOUNT_TYPE);
        LoginSessionInfo info = new LoginSessionInfo();
        boolean accountLogin = accountType == null || AccountConstant.LOGIN_TYPE.equals(accountType);
        if (accountTuples.size() == 1 && accountLogin) {
            Tuple tuple = accountTuples.get(0);
            AccountState state = tuple.get(1, AccountState.class);

            if (state == AccountState.Disabled) {
                completion.fail(err(IdentityErrors.ACCOUNT_DISABLED, "failed to login: account is disabled"));
                return;
            } else if (state == AccountState.Staled) {
                completion.fail(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password"));
                return;
            }

            String accountUuid = tuple.get(0, String.class);
            info.setAccountUuid(accountUuid);
        } else {
            completion.fail(err(IdentityErrors.AUTHENTICATION_ERROR, "wrong account name or password"));
            return;
        }

        completion.success(info);
    }

    @Override
    public Set<String> possibleAccountUuidSetForGettingProcedures(LoginContext loginContext) {
        List<String> accountUuidList = Q.New(AccountVO.class)
                .select(AccountVO_.uuid)
                .eq(AccountVO_.name, loginContext.getUsername())
                .listValues();
        return new HashSet<>(accountUuidList);
    }

    protected String getResourceIdentity(String name) {
        return Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, name).findValue();
    }

    @Override
    public boolean authenticate(String name, String password) {
        return Q.New(AccountVO.class).eq(AccountVO_.name, name)
                .eq(AccountVO_.password, password).isExists();
    }

    @Override
    public String getAccountIdByName(String username) {
        return getResourceIdentity(username);
    }

    private AccountLoginStruct getLoginEntryByName(String username, String accountType) {
        AccountVO vo = Q.New(AccountVO.class).eq(AccountVO_.name, username).find();

        boolean accountLogin = accountType == null || AccountConstant.LOGIN_TYPE.equals(accountType);
        if (vo != null && accountLogin) {
            AccountLoginStruct struct = new AccountLoginStruct();
            struct.setAccountUuid(vo.getUuid());
            struct.setLastOpTime(vo.getLastOpDate());
            return struct;
        }

        return null;
    }

    @Override
    public void collectUserInfoIntoContext(LoginContext loginContext) {
        AccountLoginStruct struct = getLoginEntryByName(loginContext.getUsername(),
                loginContext.getProperties().get(AccountConstant.ACCOUNT_TYPE));

        if (struct == null) {
            return;
        }

        loginContext.setAccountUuid(struct.getAccountUuid());
        loginContext.setLastUpdatedTime(struct.getLastOpTime());
    }

    @Override
    public List<AdditionalAuthFeature> getRequiredAdditionalAuthFeature() {
        return Arrays.asList(LoginAuthConstant.basicLoginControl, LoginAuthConstant.twoFactor);
    }
}
