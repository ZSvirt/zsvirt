package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.network.securitygroup.APIAddSecurityGroupRuleMsg.SecurityGroupRuleAO
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
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

class DeleteSecurityGroupCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3, sg4

    void testDeleteSecurityGroupCase1() {
        addVmNicToSecurityGroup {
            securityGroupUuid = sg1.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg2.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg3.uuid
            vmNicUuids = [vm1.vmNics[0].uuid]
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg4.uuid
            vmNicUuids = [vm4.vmNics[0].uuid]
        }

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Egress'
        r.description = 'sg4-egress-rule-1'
        r.protocol = 'TCP'
        r.dstPortRange = '40-400'
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            remoteSecurityGroupUuids = [sg2.uuid]
            rules = [r]
        }

        r.protocol = 'UDP'
        r.dstPortRange = '50-500'
        r.description = 'sg4-egress-rule-2'
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r]
            priority = -1
        }

        assert sg4.rules.find {it.type == 'Egress' && it.description == 'sg4-egress-rule-2' && it.priority == 2}

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 3
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg2.uuid && it.priority == 2}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 3}

        deleteSecurityGroup {
            uuid = sg2.uuid
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 2
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}

        sg4 = querySecurityGroup {
            conditions = ["uuid=${sg4.uuid}"]
        }[0]

        assert sg4.rules.find {it.type == 'Egress' && it.description == 'sg4-egress-rule-2' && it.priority == 1}
    }

    void testDeleteSecurityGroupCase2() {
        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 3

        VmNicSecurityGroupRefAO ref4 = new VmNicSecurityGroupRefAO()
        ref4.securityGroupUuid = sg4.uuid
        ref4.priority = 4

        setVmNicSecurityGroup {
            vmNicUuid = vm1.vmNics[0].uuid
            refs = [ref1, ref2, ref3]
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm2.vmNics[0].uuid
            refs = [ref1, ref2, ref3]
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm3.vmNics[0].uuid
            refs = [ref1, ref2, ref3]
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm4.vmNics[0].uuid
            refs = [ref1, ref2, ref3, ref4]
        }

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Ingress'
        r.description = 'sg4-ingress-rule-1'
        r.protocol = 'TCP'
        r.dstPortRange = '40-400'
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            remoteSecurityGroupUuids = [sg2.uuid]
            rules = [r]
        }

        r.protocol = 'TCP'
        r.dstPortRange = '50-500'
        r.description = 'sg4-ingress-rule-2'
        sg4 = addSecurityGroupRule {
            securityGroupUuid = sg4.uuid
            rules = [r]
            priority = -1
        }

        assert sg4.rules.find {it.type == 'Ingress' && it.description == 'sg4-ingress-rule-2' && it.priority == 2}


        deleteSecurityGroup {
            uuid = sg2.uuid
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm1.vmNics[0].uuid).list()
        assert refs.size() == 2
        assert refs.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}

        List<VmNicSecurityGroupRefVO> refs2 = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm2.vmNics[0].uuid).list()
        assert refs2.size() == 2
        assert refs2.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs2.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}

        List<VmNicSecurityGroupRefVO> refs3 = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm3.vmNics[0].uuid).list()
        assert refs3.size() == 2
        assert refs3.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs3.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}


        List<VmNicSecurityGroupRefVO> refs4 = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm4.vmNics[0].uuid).list()
        assert refs4.size() == 3
        assert refs4.find {it.securityGroupUuid == sg1.uuid && it.priority == 1}
        assert refs4.find {it.securityGroupUuid == sg3.uuid && it.priority == 2}
        assert refs4.find {it.securityGroupUuid == sg4.uuid && it.priority == 3}

        sg4 = querySecurityGroup {
            conditions = ["uuid=${sg4.uuid}"]
        }[0]

        assert sg4.rules.find {it.type == 'Ingress' && it.description == 'sg4-ingress-rule-2' && it.priority == 1}
    }

    void testApplyRulesWhenDeleteSecurityGroup() {
        sg2 = createSecurityGroup {
            name = "sg-2"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg2.uuid
            l3NetworkUuid = l3Net.uuid
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm3.vmNics[0].uuid
            refs = []
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm4.vmNics[0].uuid
            refs = []
        }

        VmNicSecurityGroupRefAO ref1 = new VmNicSecurityGroupRefAO()
        ref1.securityGroupUuid = sg1.uuid
        ref1.priority = 1

        VmNicSecurityGroupRefAO ref2 = new VmNicSecurityGroupRefAO()
        ref2.securityGroupUuid = sg2.uuid
        ref2.priority = 2

        VmNicSecurityGroupRefAO ref3 = new VmNicSecurityGroupRefAO()
        ref3.securityGroupUuid = sg3.uuid
        ref3.priority = 3

        setVmNicSecurityGroup {
            vmNicUuid = vm4.vmNics[0].uuid
            refs = [ref1, ref2]
        }

        setVmNicSecurityGroup {
            vmNicUuid = vm3.vmNics[0].uuid
            refs = [ref1, ref2, ref3]
        }

        SecurityGroupRuleAO r = new SecurityGroupRuleAO()
        r.type = 'Ingress'
        r.description = 'sg2-ingress-rule-1'
        r.protocol = 'TCP'
        r.dstPortRange = '40-400'
        r.remoteSecurityGroupUuid = sg3.uuid
        sg2 = addSecurityGroupRule {
            securityGroupUuid = sg2.uuid
            rules = [r]
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        deleteSecurityGroup {
            uuid = sg2.uuid
        }

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.size() == 2
            assert cmd.vmNicTOs.find {it.vmNicUuid == vm3.vmNics[0].uuid}
            assert cmd.vmNicTOs.find {it.vmNicUuid == vm4.vmNics[0].uuid}
        }
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
        env.create {
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            vm1 = env.inventoryByName("vm1") as VmInstanceInventory // vm1 in host1
            vm2 = env.inventoryByName("vm2") as VmInstanceInventory // vm2 in host2
            vm3 = env.inventoryByName("vm3") as VmInstanceInventory // vm3 in host3
            vm4 = env.inventoryByName("vm4") as VmInstanceInventory // vm4 in host3
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
                ipVersion = 4
            } as SecurityGroupInventory

            sg4 = createSecurityGroup {
                name = "sg-4"
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
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg3.uuid
                l3NetworkUuid = l3Net.uuid
            }
            attachSecurityGroupToL3Network {
                securityGroupUuid = sg4.uuid
                l3NetworkUuid = l3Net.uuid
            }
        }

        testDeleteSecurityGroupCase1()
        testDeleteSecurityGroupCase2()
        testApplyRulesWhenDeleteSecurityGroup()
    }
}
