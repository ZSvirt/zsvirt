package org.zstack.identity.imports.entity;

import org.zstack.utils.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionDSL.list;
import static org.zstack.utils.CollectionUtils.transform;

/**
 * <p>When third party source syncing, how to deal with the exists users
 * <p>
 */
public enum SyncUpdateAccountStrategy {
    /**
     * NeverUpdateState > EnableNeedChecking > AccountStateKeepSameWithSource
     */
    NeverUpdateState("state", 3),
    /**
     * If "accountSource.account.state" == "Disabled", "zsv.account.state" will update to "Disabled";
     * If "accountSource.account.state" == "Enabled", "zsv.account.state" will update to "Enabled".
     */
    AccountStateKeepSameWithSource("state", 1),
    /**
     * If "accountSource.account.state" == "Disabled", "zsv.account.state" will update to "Disabled";
     * If "accountSource.account.state" == "Enabled", "zsv.account.state" == "Disabled",
     *     then "zsv.account.state" will not change.
     */
    EnableNeedChecking("state", 2),

    /**
     * BindingNone > BindingNormalAccount > BindingSystemAdmin
     */
    BindingNone("binding", 3),
    /**
     * If "accountSource.account" is existing, and "zsv.account" (name is the same) is also existing,
     * but no relationship present, we will build relationship.
     * (maybe create {@link AccountThirdPartyAccountSourceRefVO})
     *
     * Only for Normal Accounts.
     */
    BindingNormalAccount("binding", 2),
    /**
     * If "accountSource.account" is existing, and "zsv.account" (name is the same) is also existing,
     * but no relationship present, we will build relationship.
     * (maybe create {@link AccountThirdPartyAccountSourceRefVO})
     *
     * Also supports for SystemAdmin Accounts and Normal Accounts.
     */
    BindingSystemAdmin("binding", 1),
    ;

    public final String group;
    public final int priority;
    SyncUpdateAccountStrategy(String group, int priority) {
        this.group = group;
        this.priority = priority;
    }


    public static List<SyncUpdateAccountStrategy> from(SyncCreatedAccountStrategy createStrategy) {
        switch (createStrategy) {
        case NoAction:
            return list(NeverUpdateState);
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
        if (set.contains(NeverUpdateState)) {
            set.remove(EnableNeedChecking);
            set.remove(AccountStateKeepSameWithSource);
        }
        if (set.contains(EnableNeedChecking)) {
            set.remove(AccountStateKeepSameWithSource);
        }

        if (set.contains(BindingNone)) {
            set.remove(BindingNormalAccount);
            set.remove(BindingSystemAdmin);
        }
        if (set.contains(BindingNormalAccount)) {
            set.remove(BindingSystemAdmin);
        }
        return new ArrayList<>(set);
    }

    public static List<SyncUpdateAccountStrategy> effectiveStrategies(
            List<SyncUpdateAccountStrategy> clientConfigs,
            List<SyncUpdateAccountStrategy> defaultConfigs) {

        List<SyncUpdateAccountStrategy> client = (clientConfigs == null) ? list() : simplify(clientConfigs);
        List<SyncUpdateAccountStrategy> server = (defaultConfigs == null) ? list() : simplify(defaultConfigs);
        Map<String, SyncUpdateAccountStrategy> effectiveMap = new HashMap<>();
        for (SyncUpdateAccountStrategy strategy : server) {
            updateIfHigherPriority(effectiveMap, strategy);
        }

        Set<String> clientGroups = client.stream()
                .map(it -> it.group)
                .collect(Collectors.toSet());
        clientGroups.forEach(effectiveMap::remove);
        for (SyncUpdateAccountStrategy strategy : client) {
            updateIfHigherPriority(effectiveMap, strategy);
        }

        return new ArrayList<>(effectiveMap.values());
    }

    private static void updateIfHigherPriority(
            Map<String, SyncUpdateAccountStrategy> map,
            SyncUpdateAccountStrategy candidate) {
        SyncUpdateAccountStrategy current = map.get(candidate.group);
        if (current == null || candidate.priority > current.priority) {
            map.put(candidate.group, candidate);
        }
    }

    public static List<SyncUpdateAccountStrategy> valueOfStrategies(String strategies) {
        String[] split = strategies.split(",");
        List<SyncUpdateAccountStrategy> results = new ArrayList<>();
        for (String s : split) {
            try {
                results.add(valueOf(s.trim()));
            } catch (IllegalArgumentException ignored) {}
        }
        return simplify(results);
    }

    public static String toString(List<SyncUpdateAccountStrategy> strategies) {
        return String.join(",", transform(simplify(strategies), Enum::toString).toArray(new String[0]));
    }
}
