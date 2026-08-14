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
import java.util.Collections;
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
        // Scheme B (ZSV-12379): the account/password backend is the source=Local login channel.
        // When a same-name Local account exists, it shadows any non-Local same-name accounts, so the
        // local login resolves to the Local account only. source is never declared on the login path.
        Q query = Q.New(AccountVO.class)
                .eq(AccountVO_.name, loginContext.getUsername())
                .eq(AccountVO_.password, loginContext.getPassword());
        if (hasLocalAccountNamed(loginContext.getUsername())) {
            query.eq(AccountVO_.source, AccountSource.Local);
        }
        List<Tuple> accountTuples = query
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
        AccountSource source;
        try {
            source = parseSource(loginContext.getSource());
        } catch (IllegalArgumentException e) {
            // unknown source hint: no candidates rather than leaking unrelated same-name accounts
            return Collections.emptySet();
        }

        Q query = Q.New(AccountVO.class)
                .select(AccountVO_.uuid)
                .eq(AccountVO_.name, loginContext.getUsername());
        if (source != null) {
            // explicit source selection: only honored on the GetLoginProcedures path (optional)
            query.eq(AccountVO_.source, source);
        } else if (hasLocalAccountNamed(loginContext.getUsername())) {
            // scheme B: a same-name Local account shadows non-Local ones on the local login channel
            query.eq(AccountVO_.source, AccountSource.Local);
        }
        // otherwise (no Local, no explicit source) multiple non-Local candidates may remain; the
        // caller computes the union of their procedures so that one account's configured login
        // methods are not leaked to the others.
        return new HashSet<>(query.listValues());
    }

    /**
     * Parse the optional account source hint from {@link LoginContext} (GetLoginProcedures path only).
     *
     * @return null when not provided (no source filter); otherwise the matching {@link AccountSource}
     * @throws IllegalArgumentException when a non-empty but unknown source string is provided
     */
    private AccountSource parseSource(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }
        return AccountSource.valueOf(source);
    }

    private boolean hasLocalAccountNamed(String username) {
        return Q.New(AccountVO.class)
                .eq(AccountVO_.name, username)
                .eq(AccountVO_.source, AccountSource.Local)
                .isExists();
    }

    protected String getResourceIdentity(String name) {
        // name is no longer globally unique (ZSV-12379). Prefer the same-name Local account
        // (scheme B); fall back to the first candidate to avoid NonUniqueResultException.
        String localUuid = Q.New(AccountVO.class).select(AccountVO_.uuid)
                .eq(AccountVO_.name, name)
                .eq(AccountVO_.source, AccountSource.Local)
                .limit(1).findValue();
        if (localUuid != null) {
            return localUuid;
        }
        return Q.New(AccountVO.class).select(AccountVO_.uuid).eq(AccountVO_.name, name).limit(1).findValue();
    }

    @Override
    public boolean authenticate(String name, String password) {
        // Scheme B (ZSV-12379): keep consistent with login() -- when a same-name Local account
        // exists it shadows non-Local ones on the local login channel, so a non-Local account's
        // password must not authenticate here.
        Q query = Q.New(AccountVO.class)
                .eq(AccountVO_.name, name)
                .eq(AccountVO_.password, password);
        if (hasLocalAccountNamed(name)) {
            query.eq(AccountVO_.source, AccountSource.Local);
        }
        return query.isExists();
    }

    @Override
    public String getAccountIdByName(String username) {
        return getResourceIdentity(username);
    }

    private AccountLoginStruct getLoginEntryByName(String username, String accountType) {
        boolean accountLogin = accountType == null || AccountConstant.LOGIN_TYPE.equals(accountType);
        if (!accountLogin) {
            return null;
        }

        Q query = Q.New(AccountVO.class).eq(AccountVO_.name, username);
        if (hasLocalAccountNamed(username)) {
            // scheme B: same-name Local account shadows non-Local ones on the local login channel
            query.eq(AccountVO_.source, AccountSource.Local);
        }
        // name is no longer globally unique (ZSV-12379). Only pre-fill the login struct when the
        // account can be uniquely identified; otherwise let the credential-based login resolve it.
        List<AccountVO> candidates = query.limit(2).list();
        if (candidates.size() != 1) {
            return null;
        }

        AccountVO vo = candidates.get(0);
        AccountLoginStruct struct = new AccountLoginStruct();
        struct.setAccountUuid(vo.getUuid());
        struct.setLastOpTime(vo.getLastOpDate());
        return struct;
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
