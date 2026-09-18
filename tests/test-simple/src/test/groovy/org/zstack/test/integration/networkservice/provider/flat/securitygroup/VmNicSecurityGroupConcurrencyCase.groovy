package org.zstack.test.integration.networkservice.provider.flat.securitygroup

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.core.db.SimpleQuery
import org.zstack.network.securitygroup.APISetVmNicSecurityGroupMsg.VmNicSecurityGroupRefAO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityPolicyVO
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.SecurityGroupInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.DeleteVmNicFromSecurityGroupAction
import org.zstack.sdk.AddVmNicToSecurityGroupAction
import org.zstack.sdk.SetVmNicSecurityGroupAction
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.test.integration.networkservice.provider.virtualrouter.VirtualRouterNetworkServiceEnv
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class VmNicSecurityGroupConcurrencyCase extends SubCase {
    EnvSpec env

    L3NetworkInventory l3Net
    ImageInventory image
    List<VmInstanceInventory> vmInvs = new ArrayList<>();
    List<SecurityGroupInventory> sgInvs = new ArrayList<>();
    // VmInstanceInventory vm1, vm2, vm3, vm4
    SecurityGroupInventory sg1, sg2, sg3

    void buildVmInvenceory() {
        for (int i = 1; i <= 10; i++) {
            VmInstanceInventory vm = createVmInstance {
                name = "vm-$i"
                imageUuid = image.uuid
                l3NetworkUuids = [l3Net.uuid]
                memorySize = gb(4)
                cpuNum = 2
            } as VmInstanceInventory

            vmInvs.add(vm)
        }
    }

    void buildSecurityGroup() {
        for (int i = 1; i <= 10; i++) {
            SecurityGroupInventory sg = createSecurityGroup {
                name = "sg-$i"
                ipVersion = 4
            } as SecurityGroupInventory

            attachSecurityGroupToL3Network {
                securityGroupUuid = sg.uuid
                l3NetworkUuid = l3Net.uuid
            }

            sgInvs.add(sg)
        }
    }

    void testAddVmNicToSecurityGroup1() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg1.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid]).list()
        assert refs.size() == 10
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.priority == 1} != null
        }
    }

    void testAddVmNicToSecurityGroup2() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg2.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid]).list()
        assert refs.size() == 20
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2} != null
        }
    }

    void testAddVmNicToSecurityGroup3() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                addVmNicToSecurityGroup {
                    securityGroupUuid = sg3.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid, sg3.uuid]).list()
        assert refs.size() == 30
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg1.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 2} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg3.uuid && it.priority == 3} != null
        }
    }

    void testRemoveVmNicFromSecurityGroup1() {
        def threads = []
        for (int i = 0; i < 10; i++) {
            String nicUuid = vmInvs[i].vmNics[0].uuid

            def thread = Thread.start {
                deleteVmNicFromSecurityGroup {
                    securityGroupUuid = sg1.uuid
                    vmNicUuids = [nicUuid]
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO).in(VmNicSecurityGroupRefVO_.securityGroupUuid, [sg1.uuid, sg2.uuid, sg3.uuid]).list()
        assert refs.size() == 20
        vmInvs.each { vm ->
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg2.uuid && it.priority == 1} != null
            assert refs.find {it.vmNicUuid == vm.vmNics[0].uuid && it.securityGroupUuid == sg3.uuid && it.priority == 2} != null
        }
    }

    List<VmNicSecurityGroupRefAO> buildSecurityGroupRefAO(int count) {
        List<VmNicSecurityGroupRefAO> refAOs = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            VmNicSecurityGroupRefAO ao = new VmNicSecurityGroupRefAO()
            ao.securityGroupUuid = sgUuid
            ao.priority = i
            refAOs.add(ao)
        }

        return refAOs
    }

    void testSetVmNicSecurityGroup() {
        SQL.New(VmNicSecurityPolicyVO).delete()
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            List<VmNicSecurityGroupRefAO> refAOs = buildSecurityGroupRefAO(i)
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {

                SetVmNicSecurityGroupAction action = new SetVmNicSecurityGroupAction ()
                action.vmNicUuid = nicUuid
                action.refs = refAOs
                action.sessionId = adminSession()

                SetVmNicSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }
            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
    }

    void testVmNicDetachSecurityGroup() {
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {
                DeleteVmNicFromSecurityGroupAction action = new DeleteVmNicFromSecurityGroupAction()
                action.securityGroupUuid = sgUuid
                action.vmNicUuids = [nicUuid]
                action.sessionId = adminSession()

                DeleteVmNicFromSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
        List<VmNicSecurityGroupRefVO> refVOs = Q.New(VmNicSecurityGroupRefVO).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmInvs[0].vmNics[0].uuid).list()
        assert refVOs.size() == 0
    }

    void testVmNicAttachSecurityGroup() {
        def errors = []
        def threads = []
        for (int i = 1; i <= 10; i++) {
            String sgUuid = sgInvs.get(i - 1).uuid
            String nicUuid = vmInvs[0].vmNics[0].uuid
            def thread = Thread.start {
                AddVmNicToSecurityGroupAction action = new AddVmNicToSecurityGroupAction()
                action.securityGroupUuid = sgUuid
                action.vmNicUuids = [nicUuid]
                action.sessionId = adminSession()

                AddVmNicToSecurityGroupAction.Result result = action.call()
                if (result.error != null) {
                    errors.add(result.error)
                }
            }

            threads.add(thread)
        }

        threads.each { it.join() }

        assert errors.size() == 0
        List<VmNicSecurityGroupRefVO> refVOs = Q.New(VmNicSecurityGroupRefVO).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vmInvs[0].vmNics[0].uuid).orderBy(VmNicSecurityGroupRefVO_.priority, SimpleQuery.Od.ASC).list()
        assert refVOs.size() == 10
        refVOs.each { ref ->
            assert ref.priority == refVOs.indexOf(ref) + 1
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
            image = env.inventoryByName("image") as ImageInventory
            l3Net = env.inventoryByName("l3") as L3NetworkInventory
            buildVmInvenceory()
            buildSecurityGroup()
            sg1 = sgInvs[0]
            sg2 = sgInvs[1]
            sg3 = sgInvs[2]
        }

        testAddVmNicToSecurityGroup1()
        testAddVmNicToSecurityGroup2()
        testAddVmNicToSecurityGroup3()
        testRemoveVmNicFromSecurityGroup1()
        testSetVmNicSecurityGroup()
        testVmNicDetachSecurityGroup()
        testVmNicAttachSecurityGroup()
    }
}
