package org.zstack.test.integration.networkservice.provider.flat

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMAgentCommands
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.header.vm.VmNicVO
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO
import org.zstack.network.securitygroup.VmNicSecurityGroupRefVO_
import org.zstack.network.securitygroup.VmNicSecurityTO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.flat.FlatNetworkServiceConstant

import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatUserdataBackend
import org.zstack.network.service.flat.FlatEipBackend

import org.zstack.kvm.KVMSecurityGroupBackend
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.network.IPv6Constants
import org.zstack.utils.gson.JSONObjectUtil

class FlatChangeVmIpCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf
    String userdata = "this test user data"

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }
    @Override
    void clean() {
        env.delete()
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
                    url  = "http://zstack.org/download/test.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "flatL3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                    ]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.166.100.10"
                            endIp = "192.166.100.200"
                            netmask = "255.255.255.0"
                            gateway = "192.166.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3_2"

                        ip {
                            startIp = "11.11.11.10"
                            endIp = "11.11.11.200"
                            netmask = "255.255.255.0"
                            gateway = "11.11.11.1"
                        }
                    }
                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "pubL3"
                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                    ]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        ip {
                            startIp = "12.100.10.10"
                            endIp = "12.100.10.200"
                            netmask = "255.255.255.0"
                            gateway = "12.100.10.1"
                        }
                    }

                    l3Network {
                        name = "flatL3_2"
                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(),
                                     UserdataConstant.USERDATA_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE
                                    ]
                        }
                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }
                        ip {
                            startIp = "192.167.100.10"
                            endIp = "192.167.100.200"
                            netmask = "255.255.255.0"
                            gateway = "192.167.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testFlatBaseServiceWhenChangeIp()
            testFlatBaseServiceWhenChangeL3()
            testEipWhenChangeIp()
            testUpdateNicWhenChangeL3()
            testUserdataWhenChangeIp()
            testChangeL3NetworkWithSecurityGroup()
        }
    }

    void testChangeL3NetworkWithSecurityGroup() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")

        VmInstanceInventory vm = createVmInstance {
            name = "vm-flat-change-l3"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
        }

        def sg = createSecurityGroup {
            name = "vm-test-security-group"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = flat_l3.uuid
        }
        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vm.vmNics[0].uuid]
        }

        List<VmNicSecurityGroupRefVO> refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm.vmNics[0].uuid).list()
        assert refs.size() == 1
        assert refs.find {it.securityGroupUuid == sg.uuid}.priority == 1

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).actionCode == VmNicSecurityTO.ACTION_CODE_APPLY_CHAIN
        }

        cmd = null
        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = pub_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        changeVmNicNetwork {
            vmNicUuid = vm.vmNics[0].uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        refs = Q.New(VmNicSecurityGroupRefVO.class).eq(VmNicSecurityGroupRefVO_.vmNicUuid, vm.vmNics[0].uuid).list()
        assert refs.size() == 0

        retryInSecs {
            assert cmd != null
            assert cmd.vmNicTOs.get(0).actionCode == VmNicSecurityTO.ACTION_CODE_DELETE_CHAIN
        }
    }

    void testFlatBaseServiceWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-flat-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()
        String usedIpUuid1 = vmNicVO.getUsedIps().find { it.ip == ip1 }?.uuid

        def sg = createSecurityGroup {
            name = "sg-change-l3"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = flat_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }


        FlatDhcpBackend.ReleaseDhcpCmd releaseDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            releaseDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }
        FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            batchApplyDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }
        FlatUserdataBackend.ReleaseUserdataCmd releaseUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseUserdataCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }
        FlatUserdataBackend.ApplyUserdataCmd applyUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyUserdataCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)

            return rsp
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = flat_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert ip1 != ip2
        retryInSecs {
            assert releaseDhcpCmd != null
            assert releaseDhcpCmd.dhcp.size() == 1
            assert releaseDhcpCmd.dhcp.get(0).ip == ip1
            assert batchApplyDhcpCmd != null
            assert batchApplyDhcpCmd.dhcpInfos.size() == 1
            assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.size() == 1
            assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.get(0).ip == ip2

            assert releaseUserdataCmd != null
            assert releaseUserdataCmd.namespaceName != null
            assert releaseUserdataCmd.vmIp == ip1
            assert releaseUserdataCmd.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)
            assert applyUserdataCmd != null
            assert applyUserdataCmd.userdata.vmIp == ip2
            assert applyUserdataCmd.userdata.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)

            assert cmd != null
            assert cmd.ruleTOs.get(sg.uuid) != null

            VmNicVO latestVmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
            String usedIpUuid2 = latestVmNicVO.getUsedIps().find { it.ip == ip2 }?.uuid
            assert usedIpUuid2 != null
            assert usedIpUuid2 != usedIpUuid1
            assert latestVmNicVO.getUsedIps().every { it.uuid != usedIpUuid1 }
            assert latestVmNicVO.getUsedIps().every { it.ip != ip1 }
            assert latestVmNicVO.getUsedIps().any { it.ip == ip2 && it.uuid == usedIpUuid2 }
        }
    }

    void testFlatBaseServiceWhenChangeL3() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-flat-change-l3"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
            systemTags = [VmSystemTags.USERDATA.instantiateTag([(VmSystemTags.USERDATA_TOKEN): new String(Base64.getEncoder().encode(userdata.getBytes()))])]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        def sg = createSecurityGroup {
            name = "sg-change-l3"
            ipVersion = 4
        } as SecurityGroupInventory
        attachSecurityGroupToL3Network {
            securityGroupUuid = sg.uuid
            l3NetworkUuid = flat_l3.uuid
        }
        addVmNicToSecurityGroup {
            securityGroupUuid = sg.uuid
            vmNicUuids = [vmNic.uuid]
        }

        FlatDhcpBackend.ReleaseDhcpCmd releaseDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.RELEASE_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            releaseDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.ReleaseDhcpCmd.class)
            return rsp
        }
        FlatDhcpBackend.BatchApplyDhcpCmd batchApplyDhcpCmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            batchApplyDhcpCmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }
        FlatUserdataBackend.ReleaseUserdataCmd releaseUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseUserdataCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }
        FlatUserdataBackend.ApplyUserdataCmd applyUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyUserdataCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }

        KVMAgentCommands.ApplySecurityGroupRuleCmd cmd = null
        env.afterSimulator(KVMSecurityGroupBackend.SECURITY_GROUP_APPLY_RULE_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.ApplySecurityGroupRuleCmd.class)
            return rsp
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = pub_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        retryInSecs {
            assert releaseDhcpCmd != null
            assert releaseDhcpCmd.dhcp.size() == 1
            assert releaseDhcpCmd.dhcp.get(0).ip == ip1
            assert batchApplyDhcpCmd != null
            assert batchApplyDhcpCmd.dhcpInfos.size() == 1
            assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.size() == 1
            assert batchApplyDhcpCmd.dhcpInfos.get(0).dhcp.get(0).ip == ip2

            assert releaseUserdataCmd != null
            assert releaseUserdataCmd.namespaceName != null
            assert releaseUserdataCmd.vmIp == ip1
            assert releaseUserdataCmd.bridgeName == new BridgeNameFinder().findByL3Uuid(flat_l3.uuid)
            assert applyUserdataCmd != null
            assert applyUserdataCmd.userdata.vmIp == ip2
            assert applyUserdataCmd.userdata.bridgeName == new BridgeNameFinder().findByL3Uuid(pub_l3.uuid)

            assert cmd != null
            assert cmd.vmNicTOs.get(0).actionCode == VmNicSecurityTO.ACTION_CODE_DELETE_CHAIN
        }
    }

    void testEipWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory pub_l3_2 = env.inventoryByName("pubL3_2")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-eip-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid]
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        def vip = createVip {
            name = "vip"
            l3NetworkUuid = pub_l3_2.uuid
        } as VipInventory

        def eip = createEip {
            name = "eip"
            vipUuid = vip.uuid
            vmNicUuid = vmNic.uuid
        } as EipInventory

        FlatEipBackend.DeleteEipCmd releaseEipCmd = new FlatEipBackend.DeleteEipCmd()
        env.afterSimulator(FlatEipBackend.DELETE_EIP_PATH) { rsp, HttpEntity<String> entity ->
            releaseEipCmd = json(entity.getBody(), FlatEipBackend.DeleteEipCmd.class)
            return rsp
        }
        FlatEipBackend.ApplyEipCmd applyEipCmd = new FlatEipBackend.ApplyEipCmd()
        env.afterSimulator(FlatEipBackend.APPLY_EIP_PATH) { rsp, HttpEntity<String> entity ->
            applyEipCmd = json(entity.getBody(), FlatEipBackend.ApplyEipCmd.class)
            return rsp

        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = flat_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3.uuid
            ip = ip2
        }

        assert ip1 != ip2

        retryInSecs {
            assert releaseEipCmd != null
            assert releaseEipCmd.eip.nicIp == ip1

            assert applyEipCmd != null
            assert applyEipCmd.eip.nicIp == ip2
            assert applyEipCmd.eip.vip == releaseEipCmd.eip.vip
        }
    }

    void testUpdateNicWhenChangeL3() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory flat_l3_2 = env.inventoryByName("flatL3_2")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-update-nic-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid, flat_l3_2.uuid]
            defaultL3NetworkUuid = flat_l3.uuid
        }
        VmNicInventory vmNic = vm.getVmNics().get(0)
        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        KVMAgentCommands.UpdateNicCmd updateNicCmd = null
        env.simulator(KVMConstant.KVM_UPDATE_NIC_PATH) { HttpEntity<String> e, EnvSpec espec ->
            updateNicCmd = JSONObjectUtil.toObject(e.body, KVMAgentCommands.UpdateNicCmd.class)
            return new KVMAgentCommands.UpdateNicRsp()
        }

        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = pub_l3.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        changeVmNicNetwork {
            vmNicUuid = vmNic.uuid
            destL3NetworkUuid = pub_l3.uuid
            systemTags = [String.format("staticIp::%s::%s", pub_l3.uuid, ip2)]
        }

        assert ip1 != ip2
        retryInSecs {
            assert updateNicCmd != null
            assert updateNicCmd.nics.size() == 1
            assert updateNicCmd.nics.get(0).mac == vmNicVO.getMac()
        }
    }

    void testUserdataWhenChangeIp() {
        L3NetworkInventory flat_l3 = env.inventoryByName("flatL3")
        L3NetworkInventory flat_l3_2 = env.inventoryByName("flatL3_2")
        L3NetworkInventory pub_l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = createVmInstance {
            name = "vm-userdata-change-ip"
            imageUuid = env.inventoryByName("image1").uuid
            instanceOfferingUuid = env.inventoryByName("instanceOffering").uuid
            l3NetworkUuids = [flat_l3.uuid, flat_l3_2.uuid]
            defaultL3NetworkUuid = flat_l3.uuid
        }

        VmNicInventory vmNic = CollectionUtils.findOneOrNull(vm.vmNics, { VmNicInventory it -> it.l3NetworkUuid == flat_l3_2.uuid })
        assert vmNic != null

        VmNicVO vmNicVO = dbFindByUuid(vmNic.uuid, VmNicVO.class)
        String ip1 = vmNicVO.getIp()

        assert vmNicVO != null

        FlatUserdataBackend.ReleaseUserdataCmd releaseUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.RELEASE_USER_DATA) { rsp, HttpEntity<String> e ->
                releaseUserdataCmd = json(e.body, FlatUserdataBackend.ReleaseUserdataCmd.class)
                return rsp
        }
        FlatUserdataBackend.ApplyUserdataCmd applyUserdataCmd = null
        env.afterSimulator(FlatUserdataBackend.APPLY_USER_DATA) { rsp, HttpEntity<String> e ->
            applyUserdataCmd = json(e.body, FlatUserdataBackend.ApplyUserdataCmd.class)
            return rsp
        }
        List<FreeIpInventory> freeIp4s = getFreeIp {
            l3NetworkUuid = flat_l3_2.uuid
            ipVersion = IPv6Constants.IPv4
        }
        String ip2 = freeIp4s.get(0).getIp()
        setVmStaticIp {
            vmInstanceUuid = vm.uuid
            l3NetworkUuid = flat_l3_2.uuid
            ip = ip2
        }

        assert ip1 != ip2
        retryInSecs {
            assert releaseUserdataCmd == null
            assert applyUserdataCmd == null
        }
    }
}
