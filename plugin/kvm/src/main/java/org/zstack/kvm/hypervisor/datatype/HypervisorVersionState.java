package org.zstack.kvm.hypervisor.datatype;

public enum HypervisorVersionState {
    // all virtualizer device version is matched
    Matched,

    // at least one virtualizer device version is unmatched
    Unmatched,

    // at least one virtualizer device info is unavailable,
    // and none virtualizer device version is unmatched
    Unknown
}
