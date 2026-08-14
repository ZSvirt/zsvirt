package org.zstack.identity;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.identity.AccountVO;
import org.zstack.header.identity.QuotaVO;
import org.zstack.header.identity.quota.QuotaDefinition;

import java.util.HashSet;
import java.util.Set;

import static org.zstack.core.Platform.argerr;

/**
 * Created by Wenhao.Zhang on 21/12/22
 */
public class AccountQuotaUpdateChecker implements QuotaUpdateChecker {
    @Autowired
    private AccountManager accountManager;

    @Override
    public Set<String> type() {
        Set<String> set = new HashSet<>();
        set.add(AccountVO.class.getSimpleName());
        return set;
    }

    @Override
    public ErrorCode check(QuotaVO quota, long updatedValue) {
        if (updatedValue < 0) {
            return argerr("the quota[name:%s] of account[uuid:%s] can not be %d",
                    quota.getName(), quota.getIdentityUuid(), updatedValue);
        }
        return checkQuotaChangeForAccount(quota.getIdentityUuid(), quota.getName(), updatedValue);
    }

    /**
     * check:
     *   usage <= total.updatedValue
     * TODO duplicate code with APIGetAccountQuotaUsageMsg's implement refactor requested
     */
    private ErrorCode checkQuotaChangeForAccount(String accountUuid, String quotaName, long updatedValue) {
        QuotaDefinition quota = accountManager.getQuotasDefinitions().get(quotaName);
        if (quota == null) {
            throw new CloudRuntimeException(String.format("cannot find usage[name:%s]", quotaName));
        }
        Long used = quota.getQuotaUsage(accountUuid);
        if (used == null) {
            used = 0L;
        }

        if (used > updatedValue) {
            return argerr("the account[uuid:%s] used [name:%s, usedValue:%s] exceeds request quota: %d",
                    accountUuid, quotaName, used, updatedValue);
        }

        return null;
    }
}
