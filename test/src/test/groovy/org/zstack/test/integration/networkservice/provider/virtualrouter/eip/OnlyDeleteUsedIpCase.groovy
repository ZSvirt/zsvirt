package org.zstack.test.integration.networkservice.provider.virtualrouter.eip

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQLBatch
import org.zstack.header.network.l3.L3NetworkConstant
import org.zstack.header.network.l3.ReturnIpMsg
import org.zstack.header.network.l3.UsedIpVO
import org.zstack.header.network.l3.UsedIpVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.eip.EipVO
import org.zstack.network.service.eip.EipVO_
import org.zstack.network.service.lb.LoadBalancerConstants
import org.zstack.network.service.portforwarding.PortForwardingConstant
import org.zstack.network.service.vip.VipVO
import org.zstack.network.service.vip.VipVO_
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.*
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

/**
 * Created by lining on 2017-05-25.
 */
class OnlyDeleteUsedIpCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env {

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"
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

                eip {
                    name = "eip"
                    useVip("pubL3")
                }

                virtualRouterOffering {
                    name = "vro"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }
            }
        }
    }

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void test() {
        env.create {
            testOnlyDeleteUsedIp()
            // This case only one test action, Do not add other test action
        }
    }

    void testOnlyDeleteUsedIp(){
        EipInventory eipInv = env.inventoryByName("eip") as EipInventory
        L3NetworkInventory l3Inv = env.inventoryByName("pubL3") as L3NetworkInventory
        VipVO vipVO = dbFindByUuid(eipInv.vipUuid, VipVO.class)
        CloudBus bus = bean(CloudBus.class)

        ReturnIpMsg rmsg = new ReturnIpMsg()
        rmsg.usedIpUuid = vipVO.usedIpUuid
        rmsg.l3NetworkUuid = l3Inv.uuid
        bus.makeTargetServiceIdByResourceUuid(rmsg, L3NetworkConstant.SERVICE_ID, l3Inv.uuid)
        assert bus.call(rmsg).success
        retryInSecs(){
            new SQLBatch(){
                @Override
                protected void scripts() {
                    assert q(EipVO.class).eq(EipVO_.uuid, eipInv.uuid).isExists()
                    assert q(VipVO.class).eq(VipVO_.uuid, vipVO.uuid).isExists()
                    assert !q(UsedIpVO.class).eq(UsedIpVO_.uuid, vipVO.usedIpUuid).isExists()
                }
            }.execute()
        }
    }
}
