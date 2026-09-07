package org.zstack.testlib.premium.dbremaining

import org.zstack.core.db.Q
import org.zstack.header.affinitygroup.AffinityGroupConstants
import org.zstack.header.affinitygroup.AffinityGroupVO
import org.zstack.header.affinitygroup.AffinityGroupVO_
import org.zstack.header.identity.AccountResourceRefVO
import org.zstack.header.vmscheduling.VmSchedulingRuleGroupVO
import org.zstack.header.vmscheduling.VmSchedulingRuleGroupVO_
import org.zstack.header.vmscheduling.VmSchedulingRuleRefVO
import org.zstack.header.vmscheduling.VmSchedulingRuleVO
import org.zstack.testlib.dbremaining.AllowedDBRemaining

class VmSchedulingDBRemaining extends AllowedDBRemaining {
    @Override
    void remaining() {
        table {
            tableVOClass = VmSchedulingRuleGroupVO.class
            checker = { List<VmSchedulingRuleGroupVO> vos ->
                return vos.findAll { it.name != AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP }
            }
        }

        table {
            tableVOClass = AffinityGroupVO.class
            checker = { List<AffinityGroupVO> vos ->
                return vos.findAll { it.name != AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP }
            }
        }

        table {
            tableVOClass = VmSchedulingRuleVO.class
            checker = { List<VmSchedulingRuleVO> vos ->
                List<String> defaultRuleUuids = Q.New(VmSchedulingRuleGroupVO.class)
                        .eq(VmSchedulingRuleGroupVO_.name, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                        .select(VmSchedulingRuleGroupVO_.srcUuid)
                        .listValues()
                return vos.findAll {
                    it.name != AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP &&
                            !defaultRuleUuids.contains(it.uuid)
                }
            }
        }

        table {
            tableVOClass = VmSchedulingRuleRefVO.class
            checker = { List<VmSchedulingRuleRefVO> vos ->
                List<String> defaultGroupUuids = Q.New(VmSchedulingRuleGroupVO.class)
                        .eq(VmSchedulingRuleGroupVO_.name, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                        .select(VmSchedulingRuleGroupVO_.uuid)
                        .listValues()
                List<String> defaultRuleUuids = Q.New(VmSchedulingRuleGroupVO.class)
                        .eq(VmSchedulingRuleGroupVO_.name, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                        .select(VmSchedulingRuleGroupVO_.srcUuid)
                        .listValues()
                return vos.findAll {
                    !defaultGroupUuids.contains(it.vmGroupUuid) &&
                            !defaultRuleUuids.contains(it.vmSchedulingRuleUuid)
                }
            }
        }

        table {
            tableVOClass = AccountResourceRefVO.class
            checker = { List<AccountResourceRefVO> vos ->
                List<String> uuids = []
                List<VmSchedulingRuleGroupVO> defaultGroups = Q.New(VmSchedulingRuleGroupVO.class)
                        .eq(VmSchedulingRuleGroupVO_.name, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                        .list()
                defaultGroups.each {
                    uuids.add(it.uuid)
                    if (it.srcUuid != null) {
                        uuids.add(it.srcUuid)
                    }
                }
                uuids.addAll(Q.New(AffinityGroupVO.class)
                        .eq(AffinityGroupVO_.name, AffinityGroupConstants.VIRTUAL_ROUTER_AFFINITY_GROUP)
                        .select(AffinityGroupVO_.uuid)
                        .listValues())
                return vos.findAll { !uuids.contains(it.resourceUuid) }
            }
        }
    }
}
