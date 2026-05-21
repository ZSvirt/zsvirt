package org.zstack.identity.imports.entity;

import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zstack.utils.CollectionDSL.list;

/**
 * <p>When third party source syncing, how to deal with the exists users
 * <p>
 */
public enum SyncUpdateAccountStrategy {
    /**
     * If "accountSource.account.state" == "Disabled", "zsv.account.state" will update to "Disabled";
     * If "accountSource.account.state" == "Enabled", "zsv.account.state" will update to "Enabled".
     */
    AccountStateKeepSameWithSource,
    /**
     * If "accountSource.account.state" == "Disabled", "zsv.account.state" will update to "Disabled";
     * If "accountSource.account.state" == "Enabled", "zsv.account.state" == "Disabled",
     *     then "zsv.account.state" will not change.
     *
     * If {@link #AccountStateKeepSameWithSource} and {@link #EnableNeedChecking} are both present,
     * {@link #AccountStateKeepSameWithSource} will take effort.
     */
    EnableNeedChecking,
    /**
     * If "accountSource.account" is existing, and "zsv.account" (name is the same) is also existing,
     * but no relationship present, we will build relationship.
     * (maybe create {@link AccountThirdPartyAccountSourceRefVO})
     *
     * Only for Normal Accounts.
     */
    BindingNormalAccount,
    /**
     * If "accountSource.account" is existing, and "zsv.account" (name is the same) is also existing,
     * but no relationship present, we will build relationship.
     * (maybe create {@link AccountThirdPartyAccountSourceRefVO})
     *
     * Also supports for SystemAdmin Accounts (include Normal accounts, BindingSystemAdmin include BindingNormalAccount).
     */
    BindingSystemAdmin,
    ;

    public static List<SyncUpdateAccountStrategy> from(SyncCreatedAccountStrategy createStrategy) {
        switch (createStrategy) {
        case NoAction:
            return list();
        case CreateDisabledAccount:
            return list(EnableNeedChecking);
        case CreateAccount: default:
            return list(AccountStateKeepSameWithSource);
        }
    }

    public static List<SyncUpdateAccountStrategy> simplify(List<SyncUpdateAccountStrategy> rawList) {
        if (CollectionUtils.isEmpty(rawList)) {
            return new ArrayList<>();
        }

        Set<SyncUpdateAccountStrategy> set = new HashSet<>(rawList);
        if (set.contains(AccountStateKeepSameWithSource)) {
            set.remove(EnableNeedChecking);
        }
        if (set.contains(BindingSystemAdmin)) {
            set.remove(BindingNormalAccount);
        }
        return new ArrayList<>(set);
    }
}
