package org.zstack.header.affinitygroup;

import org.zstack.header.vm.VmInstanceState;

public class AffinityGroupConstants {
    public static final String SERVICE_ID = "affinityGroup";
    public static final String ACTION_CATEGORY = "affinity-groups";
    public static final VmInstanceState[] vmNotValidStates = {VmInstanceState.Stopping,
            VmInstanceState.Stopped, VmInstanceState.Destroying,
            VmInstanceState.Destroyed, VmInstanceState.Expunging,
            VmInstanceState.Error, VmInstanceState.Unknown};
    public static final String DEFAULT_VERSION = "1.0";
    public static final String VIRTUAL_ROUTER_AFFINITY_GROUP="zstack.affinity.group.for.virtual.router";
    public static final String AFFINITY_GROUP_RESERVE_ID = "AffinityGroupReserveFlow";

    public enum Param {
        RESERVE_SUCCESS,
        ORIGIN_HOST_UUID
    }

    public static final String AFFINITYGROUP_NUM = "affinitygroup.num";
}
