package org.zstack.test.integration.networkservice.provider.virtualrouter.portforwarding

import org.springframework.http.HttpEntity
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.portforwarding.PortForwardingProtocolType
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO
import org.zstack.network.service.virtualrouter.vip.VirtualRouterVipVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.PortForwardingRuleInventory
import org.zstack.sdk.VipInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
/**
 * Created by AlanJager on 2017/4/21.
 */
class PortForwardingCase extends SubCase {
    EnvSpec env

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
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "vr"
                    url = "http://zstack.org/download/vr.qcow2"
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
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3"

                        service {
                            provider = VyosConstants.VYOS_ROUTER_PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(),
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString(),
                                     PortForwardingConstant.PORTFORWARDING_NETWORK_SERVICE_TYPE,
                                     LoadBalancerConstants.LB_NETWORK_SERVICE_TYPE_STRING,
                                     EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3"

                        ip {
                            startIp = "11.168.100.10"
                            endIp = "11.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "11.168.100.1"
                        }
                    }
                }

                attachBackupStorage("sftp")

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }

            vm {
                name = "vm"
                useImage("image")
                useL3Networks("l3")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testDeleteOneUnattchedPortForwadingRuleAndAttachAnotherWillRequireVip()
            testDeletePortForwardingRuleWhenNotAttachAnyVm()
        }
    }

    void testDeleteOneUnattchedPortForwadingRuleAndAttachAnotherWillRequireVip() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        deletePortForwardingRule {
            uuid = portForwarding.getUuid()
        }
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0

        VirtualRouterCommands.RemoveVipCmd removeVipCmd = null
        env.afterSimulator(VirtualRouterConstant.VR_REMOVE_VIP) { VirtualRouterCommands.RemoveVipRsp rsp, HttpEntity<String> e ->
            removeVipCmd = JSONObjectUtil.toObject(e.body,  VirtualRouterCommands.RemoveVipCmd.class)
            return rsp
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding2.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1

        deletePortForwardingRule {
            uuid = portForwarding2.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert removeVipCmd != null

        deleteVip {
            uuid = vip.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()
    }

    void testDeletePortForwardingRuleWhenNotAttachAnyVm() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
        }

        attachPortForwardingRule {
            vmNicUuid = vm.getVmNics().get(0).uuid
            ruleUuid = portForwarding.uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1
        VipVO vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
        }
        vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null

        deletePortForwardingRule {
            uuid = portForwarding.getUuid()
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider == null

        deletePortForwardingRule {
            uuid = portForwarding2.getUuid()
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 0
        assert Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()

        deleteVip {
            uuid = vip.uuid
        }
        assert !Q.New(VipVO.class).eq(VipVO_.uuid, vip.uuid).isExists()
    }

    void testCreatePortForwardingRuleWillNotSetVipServiceProviderToNull() {
        L3NetworkInventory l3 = env.inventoryByName("pubL3")
        VmInstanceInventory vm = env.inventoryByName("vm")
        VipInventory vip = createVip {
            name = "vip"
            l3NetworkUuid = l3.uuid
        }

        assert Q.New(VipVO.class).select(VipVO_.uuid).eq(VipVO_.uuid, vip.uuid).listValues().size() == 1
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().isEmpty()

        PortForwardingRuleInventory portForwarding = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 22
            vipPortEnd = 22
            privatePortEnd = 100
            privatePortStart = 100
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.getVmNics().get(0).uuid
        }
        assert Q.New(VirtualRouterVipVO.class).select(VirtualRouterVipVO_.uuid).eq(VirtualRouterVipVO_.uuid, vip.uuid).listValues().size() == 1

        stopVmInstance {
            uuid = vm.uuid
        }
        VmInstanceVO vmInstanceVO = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vmInstanceVO.state == VmInstanceState.Stopped

        PortForwardingRuleInventory portForwarding2 = createPortForwardingRule {
            name = "test"
            vipUuid = vip.uuid
            vipPortStart = 3306
            vipPortEnd = 3306
            privatePortEnd = 3306
            privatePortStart = 3306
            protocolType = PortForwardingProtocolType.TCP.toString()
            vmNicUuid = vm.vmNics.get(0).uuid
        }
        VipVO vipVO = dbFindByUuid(vip.uuid, VipVO.class)
        assert vipVO.serviceProvider != null
    }
}
