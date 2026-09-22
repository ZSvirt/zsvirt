package org.zstack.header.allocator.domain;

import java.io.Serializable;

/**
 * One explicit host-allocation rule inside an {@link AllocatorHostRuleSet}.
 * Phase 1 leaves this unused; domain constraints use {@link AllocatorHostRuleSet#domainUuid}.
 */
public class AllocatorHostRule implements Serializable {
    private static final long serialVersionUID = 1L;

    public String effect;
    public String resourceType;
    public String resourceUuid;
    public String resourceQualifier;

    public AllocatorHostRule() {
    }

    @Override
    public String toString() {
        return effect + " <" + resourceType + ">" + resourceUuid
                + " (qualifier=" + resourceQualifier + ")";
    }
}
