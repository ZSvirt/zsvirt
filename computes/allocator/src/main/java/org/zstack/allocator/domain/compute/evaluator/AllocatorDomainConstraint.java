package org.zstack.allocator.domain.compute.evaluator;

import org.zstack.allocator.domain.entity.AllocatorDomainVO;

/**
 * Allocation-time decision: whether this scene constrains the VM, and with which domain.
 * Does not write ruleSets, filter hosts, or change tags.
 */
public class AllocatorDomainConstraint {
    public enum Kind {
        NONE,
        DISABLED,
        SCENE_NOT_IN_TABLE,
        ACTIVE,
    }

    public final Kind kind;
    public final AllocatorDomainVO domain;

    private AllocatorDomainConstraint(Kind kind, AllocatorDomainVO domain) {
        this.kind = kind;
        this.domain = domain;
    }

    public static AllocatorDomainConstraint none() {
        return new AllocatorDomainConstraint(Kind.NONE, null);
    }

    public static AllocatorDomainConstraint disabled(AllocatorDomainVO domain) {
        return new AllocatorDomainConstraint(Kind.DISABLED, domain);
    }

    public static AllocatorDomainConstraint sceneNotInTable(AllocatorDomainVO domain) {
        return new AllocatorDomainConstraint(Kind.SCENE_NOT_IN_TABLE, domain);
    }

    public static AllocatorDomainConstraint active(AllocatorDomainVO domain) {
        return new AllocatorDomainConstraint(Kind.ACTIVE, domain);
    }
}
