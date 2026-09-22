package org.zstack.header.allocator.domain;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * One named constraint set on {@link org.zstack.header.allocator.AllocateHostMsg}.
 * Sets are AND-ed in list order. Append only; do not overwrite another source's set.
 *
 * Phase 1 carries the allocator domain as {@link #NAME_DOMAIN} + {@link #domainUuid}.
 * Later sources (resource binding, affinity) append their own sets with {@link #rules}.
 */
public class AllocatorHostRuleSet implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final String NAME_DOMAIN = "domain";

    public String name;
    public String domainUuid;
    public String zoneUuid;
    public List<AllocatorHostRule> rules = new ArrayList<>();

    public AllocatorHostRuleSet() {
    }

    public static AllocatorHostRuleSet domain(String domainUuid) {
        AllocatorHostRuleSet set = new AllocatorHostRuleSet();
        set.name = NAME_DOMAIN;
        set.domainUuid = domainUuid;
        return set;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[').append(name).append(']');
        if (domainUuid != null && !domainUuid.isEmpty()) {
            sb.append("\n* domain=").append(domainUuid);
        }
        if (zoneUuid != null && !zoneUuid.isEmpty()) {
            sb.append("\n* zone=").append(zoneUuid);
        }
        if (rules != null) {
            for (AllocatorHostRule rule : rules) {
                if (rule == null) {
                    continue;
                }
                sb.append("\n* ").append(rule);
            }
        }
        return sb.toString();
    }

    public static String toString(List<AllocatorHostRuleSet> ruleSets) {
        if (ruleSets == null || ruleSets.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < ruleSets.size(); i++) {
            if (i > 0) {
                sb.append('\n');
            }
            AllocatorHostRuleSet set = ruleSets.get(i);
            sb.append(set == null ? "null" : set.toString());
        }
        return sb.toString();
    }
}
