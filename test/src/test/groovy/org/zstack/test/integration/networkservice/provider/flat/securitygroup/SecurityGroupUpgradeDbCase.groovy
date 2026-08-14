package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO_
import org.zstack.network.securitygroup.SecurityGroupGlobalProperty
import org.zstack.network.securitygroup.SecurityGroupRuleVO
import org.zstack.network.securitygroup.SecurityGroupRuleVO_
import org.zstack.network.securitygroup.SecurityGroupVO
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.securitygroup.SecurityGroupRuleType
import org.zstack.network.securitygroup.SecurityGroupRuleProtocolType
import org.zstack.network.securitygroup.SecurityGroupRuleState
import org.zstack.network.securitygroup.SecurityGroupUpgradeExtension
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.SecurityGroupRuleInventory
import org.zstack.sdk.VmNicSecurityGroupRefInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicSecurityPolicyInventory
import org.zstack.header.apimediator.ApiMessageInterceptionException
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil
import java.util.concurrent.TimeUnit

class SecurityGroupUpgradeDbCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf
    SecurityGroupUpgradeExtension upgradeExt
    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void testUpgradeSecurityGroupRules() {
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
            }
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Egress"
            r.ipVersion = 4
            r.startPort = i
            r.endPort = i
            r.protocol = "UDP"

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
            }
        }

        SecurityGroupRuleAO ingressAllRule = new SecurityGroupRuleAO()
        ingressAllRule.type = 'Ingress'
        ingressAllRule.ipVersion = 4
        ingressAllRule.protocol = 'ALL'

        addSecurityGroupRule {
            securityGroupUuid = sg1.uuid
            rules = [ingressAllRule]
        }

        List<SecurityGroupRuleVO> oldIngressRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).list()
        assert oldIngressRules.find {it.ipVersion == 4 && it.protocol == SecurityGroupRuleProtocolType.ALL && it.remoteSecurityGroupUuid == null}
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid)
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid)
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()
        
        upgradeExt.start()

        List<SecurityGroupRuleVO> ingressRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress).list()
        List<SecurityGroupRuleVO> egressRules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Egress).list()

        assert ingressRules.find {it.priority == 0 && it.ipVersion == 4 && it.description == "default rule"}
        assert ingressRules.find {it.priority == 0 && it.ipVersion == 6 && it.description == "default rule"}
        assert ingressRules.find {it.priority == 1}
        assert ingressRules.find {it.priority == 2}
        assert ingressRules.find {it.priority == 3}
        assert ingressRules.find {it.priority == 4}
        assert ingressRules.find {it.priority == 5}
        assert ingressRules.find {it.ipVersion == 4 && it.protocol == SecurityGroupRuleProtocolType.ALL && it.remoteSecurityGroupUuid == null && it.description == null}

        assert egressRules.find {it.priority == 0 && it.ipVersion == 4 && it.description == "default rule"}
        assert egressRules.find {it.priority == 0 && it.ipVersion == 6 && it.description == "default rule"}
        assert egressRules.find {it.priority == 1}
        assert egressRules.find {it.priority == 2}
        assert egressRules.find {it.priority == 3}
        assert egressRules.find {it.priority == 4}
        assert egressRules.find {it.priority == 5}

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class)
                .eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 2
        assert refs.find {it.priority == 1}
        assert refs.find {it.priority == 2}
        assert Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid).isExists()

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testUpgradeSecurityGroupWithNoRules() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            addSecurityGroupRule {
                securityGroupUuid = sg2.uuid
                rules = [r]
            }
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .eq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .delete()
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .eq(SecurityGroupRuleVO_.type, SecurityGroupRuleType.Ingress)
                .eq(SecurityGroupRuleVO_.ipVersion, 4)
                .eq(SecurityGroupRuleVO_.priority, SecurityGroupConstant.DEFAULT_RULE_PRIORITY)
                .delete()
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()

        upgradeExt.start()

        List<SecurityGroupRuleVO> sg1Rules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid).list()

        assert sg1Rules.size() == 4
        assert sg1Rules.find {it.priority == 0 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Ingress && it.state == SecurityGroupRuleState.Disabled}
        assert sg1Rules.find {it.priority == 0 && it.ipVersion == 6 && it.type == SecurityGroupRuleType.Ingress && it.state == SecurityGroupRuleState.Disabled}
        assert sg1Rules.find {it.priority == 0 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Egress && it.state == SecurityGroupRuleState.Disabled}
        assert sg1Rules.find {it.priority == 0 && it.ipVersion == 6 && it.type == SecurityGroupRuleType.Egress && it.state == SecurityGroupRuleState.Disabled}

        List<SecurityGroupRuleVO> sg2Rules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid).list()

        assert sg2Rules.size() == 9
        assert sg2Rules.find {it.priority == 0 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Ingress && it.state == SecurityGroupRuleState.Disabled}
        assert sg2Rules.find {it.priority == 0 && it.ipVersion == 6 && it.type == SecurityGroupRuleType.Ingress && it.state == SecurityGroupRuleState.Enabled}
        assert sg2Rules.find {it.priority == 0 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Egress && it.state == SecurityGroupRuleState.Enabled}
        assert sg2Rules.find {it.priority == 0 && it.ipVersion == 6 && it.type == SecurityGroupRuleType.Egress && it.state == SecurityGroupRuleState.Enabled}

        SecurityGroupRuleVO userRule = sg2Rules.find {it.priority == 3 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Ingress}
        assert userRule != null
        SecurityGroupRuleInventory ruleInv = changeSecurityGroupRule {
            uuid = userRule.uuid
            remoteSecurityGroupUuid = sg1.uuid
            priority = 1
        }

        assert ruleInv.remoteSecurityGroupUuid == sg1.uuid
        assert ruleInv.priority == 1

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testAddSecurityGroupRuleAfterUpgradeDB() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = "Ingress"
            r.ipVersion = 4
            r.protocol = "TCP"
            r.startPort = i
            r.endPort = i

            addSecurityGroupRule {
                securityGroupUuid = sg2.uuid
                rules = [r]
            }
        }

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = 'Egress'
            r.ipVersion = 4
            r.protocol = 'ICMP'
            r.dstIpRange = String.format('10.0.0.%s', i)

            addSecurityGroupRule {
                securityGroupUuid = sg2.uuid
                rules = [r]
            }
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        upgradeExt.start()

        List<SecurityGroupRuleVO> sg1Rules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid).list()

        assert sg1Rules.size() == 4
        assert sg1Rules.every {it.priority == 0}

        List<SecurityGroupRuleVO> sg2Rules = Q.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid).list()

        assert sg2Rules.size() == 14

        SecurityGroupRuleVO ingressRule_1 = sg2Rules.find {it.priority == 1 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Ingress}
        SecurityGroupRuleVO egressRule_1 = sg2Rules.find {it.priority == 1 && it.ipVersion == 4 && it.type == SecurityGroupRuleType.Egress}

        SecurityGroupRuleAO r1 = new SecurityGroupRuleAO()
        r1.type = 'Ingress'
        r1.ipVersion = 4
        r1.protocol = 'UDP'
        r1.dstPortRange = '10-20'

        SecurityGroupRuleAO r2 = new SecurityGroupRuleAO()
        r2.type = 'Egress'
        r2.ipVersion = 4
        r2.protocol = 'TCP'
        r2.dstPortRange = '30-40'

        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r1, r2]
            priority = 1
        }

        SecurityGroupRuleInventory rule_1 = sg2.rules.find {it.priority == 1 && it.ipVersion == 4 && it.type == 'Ingress' && it.protocol == 'UDP' && it.dstPortRange == '10-20'}
        SecurityGroupRuleInventory rule_2 = sg2.rules.find {it.priority == 1 && it.ipVersion == 4 && it.type == 'Egress' && it.protocol == 'TCP' && it.dstPortRange == '30-40'}
        assert rule_1 != null
        assert rule_2 != null
        assert sg2.rules.find {it.uuid == ingressRule_1.uuid && it.priority == 2}
        assert sg2.rules.find {it.uuid == egressRule_1.uuid && it.priority == 2}

        sg2 = deleteSecurityGroupRule {
            ruleUuids = [rule_1.uuid, rule_2.uuid]
        }

        assert sg2.rules.find {it.uuid == ingressRule_1.uuid && it.priority == 1}
        assert sg2.rules.find {it.uuid == egressRule_1.uuid && it.priority == 1}
        assert sg2.rules.find {it.uuid == rule_1.uuid } == null
        assert sg2.rules.find {it.uuid == rule_2.uuid } == null

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testAttachNicToSecurityGroupAfterUpgradeDB() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        upgradeExt.start()

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]
        }

        SQL.New(VmNicSecurityPolicyVO.class)
                .eq(VmNicSecurityPolicyVO_.vmNicUuid, vm2.vmNics[0].uuid)
                .delete()

        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        List<VmNicSecurityPolicyVO> policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()

        assert refs.size() == 3
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}

        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}

        assert policies.size() == 2
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}
        assert policies.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        deleteVmNicFromSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm2.vmNics[0].uuid]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        assert refs.size() == 2
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid} == null
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 1}

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testSetVmNicSecurityGroupAfterUpgradeDB() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        upgradeExt.start()

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        setVmNicSecurityGroup {
            vmNicUuid = vm1.vmNics[0].uuid
            refs = [ref1]
        }

        List<VmNicSecurityGroupRefVO> refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        List<VmNicSecurityPolicyVO> policies = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid).list()

        assert refvos.size() == 1
        assert refvos.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert policies.size() == 1
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        SQL.New(VmNicSecurityPolicyVO.class)
                .eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid)
                .delete()

        setVmNicSecurityGroup {
            vmNicUuid = vm1.vmNics[0].uuid
            refs = [ref1, ref2]
        }

        refvos = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        policies = Q.New(VmNicSecurityPolicyVO.class).eq(VmNicSecurityPolicyVO_.vmNicUuid, vm1.vmNics[0].uuid).list()

        assert refvos.size() == 2
        assert refvos.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refvos.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert policies.size() == 1
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testUpdateDBMutiple() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg1.uuid
            l3NetworkUuid = l3Net.uuid
        }

        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]
        }

        TimeUnit.SECONDS.sleep(2)

        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .set(SecurityGroupRuleVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .set(SecurityGroupRuleVO_.dstPortRange, null)
                .set(SecurityGroupRuleVO_.description, null)
                .update()
        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        upgradeExt.start()

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        List<VmNicSecurityPolicyVO> policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        List<SecurityGroupRuleVO> rvos = Q.New(SecurityGroupRuleVO.class).in(SecurityGroupRuleVO_.securityGroupUuid, [sg1.uuid, sg2.uuid]).list()

        assert refs.size() == 4
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}

        assert policies.size() == 2
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}
        assert policies.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        assert rvos.size() == 8
        assert rvos.every {it.priority == 0}

        upgradeExt.start()

        refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        rvos = Q.New(SecurityGroupRuleVO.class).in(SecurityGroupRuleVO_.securityGroupUuid, [sg1.uuid, sg2.uuid]).list()

        assert refs.size() == 4
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}

        assert policies.size() == 2
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}
        assert policies.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        assert rvos.size() == 8
        assert rvos.every {it.priority == 0}

        for (int i = 1; i <= 5; i++) {
            SecurityGroupRuleAO r = new SecurityGroupRuleAO()
            r.type = 'Egress'
            r.ipVersion = 4
            r.protocol = 'ICMP'
            r.dstIpRange = String.format('10.0.0.%s', i)

            addSecurityGroupRule {
                securityGroupUuid = sg1.uuid
                rules = [r]
            }

            addSecurityGroupRule {
                securityGroupUuid = sg2.uuid
                rules = [r]
            }
        }

        upgradeExt.start()
        upgradeExt.start()
        upgradeExt.start()

        refs = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        policies = Q.New(VmNicSecurityPolicyVO.class).in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid]).list()
        rvos = Q.New(SecurityGroupRuleVO.class).in(SecurityGroupRuleVO_.securityGroupUuid, [sg1.uuid, sg2.uuid]).list()

        assert refs.size() == 4
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2}

        assert policies.size() == 2
        assert policies.find {it.vmNicUuid == vm1.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}
        assert policies.find {it.vmNicUuid == vm2.vmNics[0].uuid && it.ingressPolicy == 'DENY' && it.egressPolicy == 'ALLOW'}

        assert rvos.size() == 18
        def priorities = [1, 2, 3, 4, 5]
        for(int priority : priorities){
            assert rvos.find {it.securityGroupUuid == sg1.uuid && it.priority == priority}
        }
        for(int priority : priorities){
            assert rvos.find {it.securityGroupUuid == sg2.uuid && it.priority == priority}
        }

        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
    }

    void testDeadlockAfterUpgradeDB() {
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        List<VmInstanceInventory> vmInvs = [vm1, vm2, vm3, vm4]

        def threads = []
        for (int i = 0; i < 4; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                setVmNicSecurityGroup {
                    vmNicUuid = nicUuid
                    refs = [ref1, ref2]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refvos = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid, vm4.vmNics[0].uuid]).list()
        assert refvos.size() == 8
        
        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .delete()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .delete()

        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid, vm4.vmNics[0].uuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid, vm4.vmNics[0].uuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        upgradeExt.start()

        def threads2 = []
        for (int i = 0; i < 4; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                setVmNicSecurityGroup {
                    vmNicUuid = nicUuid
                    refs = []
                }
            }
            threads2.add(thread)
        }

        threads2.each { it.join() }

        refvos = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [vm1.vmNics[0].uuid, vm2.vmNics[0].uuid, vm3.vmNics[0].uuid, vm4.vmNics[0].uuid]).list()
        assert refvos.size() == 0
    }

    void testDuplicateRefAfterUpgradeDB() {
        deleteSecurityGroup {
            uuid = sg1.uuid
        }
        deleteSecurityGroup {
            uuid = sg2.uuid
        }
        deleteSecurityGroup {
            uuid = sg3.uuid
        }

        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true
        sg1 = createSecurityGroup {
            name = "sg-1"
            ipVersion = 4
        } as SecurityGroupInventory

        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 6
        } as SecurityGroupInventory

        sg3 = createSecurityGroup {
            name = "sg-3"
            ipVersion = 6
        } as SecurityGroupInventory

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 3

        String nicUuid = vm4.vmNics[0].uuid
        setVmNicSecurityGroup {
            vmNicUuid = nicUuid
            refs = [ref1, ref2, ref3]
        }

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg1.uuid)
                .delete()

        SQL.New(SecurityGroupRuleVO.class)
                .eq(SecurityGroupRuleVO_.securityGroupUuid, sg2.uuid)
                .delete()

        SQL.New(VmNicSecurityPolicyVO.class)
                .in(VmNicSecurityPolicyVO_.vmNicUuid, [nicUuid])
                .delete()
        SQL.New(VmNicSecurityGroupRefVO.class)
                .in(VmNicSecurityGroupRefVO_.vmNicUuid, [nicUuid])
                .set(VmNicSecurityGroupRefVO_.priority, SecurityGroupConstant.LOWEST_RULE_PRIORITY)
                .update()

        List<VmNicSecurityGroupRefVO> refvos = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [nicUuid]).list()
        assert refvos.size() == 3

        VmNicSecurityGroupRefVO vo1 = new VmNicSecurityGroupRefVO()
        vo1.setUuid(Platform.getUuid())
        vo1.setVmNicUuid(nicUuid)
        vo1.setVmInstanceUuid(vm4.uuid)
        vo1.setSecurityGroupUuid(sg1.uuid)
        vo1.setPriority(-1)
        dbf.persist(vo1)

        VmNicSecurityGroupRefVO vo2 = new VmNicSecurityGroupRefVO()
        vo2.setUuid(Platform.getUuid())
        vo2.setVmNicUuid(nicUuid)
        vo2.setVmInstanceUuid(vm4.uuid)
        vo2.setSecurityGroupUuid(sg2.uuid)
        vo2.setPriority(-1)
        dbf.persist(vo2)

        refvos = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [nicUuid]).list()
        assert refvos.size() == 5

        upgradeExt.start()

        refvos = Q.New(VmNicSecurityGroupRefVO.class).in(VmNicSecurityGroupRefVO_.vmNicUuid, [nicUuid]).list()
        assert refvos.size() == 3
        assert refvos.find{it.securityGroupUuid == sg1.uuid}
        assert refvos.find{it.securityGroupUuid == sg2.uuid}
        assert refvos.find{it.securityGroupUuid == sg3.uuid}
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = VirtualRouterNetworkServiceEnv.fourVmThreeHostNoEipForSecurityGroupEnv()
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        upgradeExt = bean(SecurityGroupUpgradeExtension.class)
        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = true

        env.create {
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory

            sg3 = createSecurityGroup {
                name = "sg-3"
                ipVersion = 4
            } as SecurityGroupInventory
        }

        testUpgradeSecurityGroupRules()
        testUpgradeSecurityGroupWithNoRules()
        testAddSecurityGroupRuleAfterUpgradeDB()
        testAttachNicToSecurityGroupAfterUpgradeDB()
        testSetVmNicSecurityGroupAfterUpgradeDB()
        testUpdateDBMutiple()
        testDeadlockAfterUpgradeDB()
        testDuplicateRefAfterUpgradeDB()

        SecurityGroupGlobalProperty.UPGRADE_SECURITY_GROUP = false
    }
}
