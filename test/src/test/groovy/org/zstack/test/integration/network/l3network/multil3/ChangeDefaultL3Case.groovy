package org.zstack.test.integration.network.l3network.multil3

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.BridgeNameFinder
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VmNicInventory

import org.zstack.test.integration.network.NetworkTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.CollectionUtils
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.TimeUnit

/**
 * Created by heathhose on 17-3-25.
 */
class ChangeDefaultL3Case extends SubCase{
    def DOC = """
use:
1. create a vm with 2 flat L3 networks
2. change the vm's default L3 network from one to another
3. confirm the ResetDefaultGatewayCmd sent to the backend
"""
    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
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
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2-1")
                    attachL2Network("l2-2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth1"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                }

                l2NoVlanNetwork {
                    name = "l2-2"
                    physicalInterface = "eth2"

                    l3Network {
                        name = "l3-2"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), EipConstant.EIP_NETWORK_SERVICE_TYPE, UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        ip {
                            startIp = "192.168.200.10"
                            endIp = "192.168.200.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.200.1"
                        }
                    }

                    l3Network {
                        name = "l3-3"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.203.10"
                            endIp = "192.168.203.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.203.1"
                        }
                    }

                    l3Network {
                        name = "l3-4"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE]
                        }

                        ip {
                            startIp = "192.168.204.10"
                            endIp = "192.168.204.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.204.1"
                        }
                    }

                }

                attachBackupStorage("sftp")

            }

            vm {
                name = "vm"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1","l3-2", "l3-3", "l3-4")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            changeDefaultL3NetworkToAnother()
        }
    }

    void changeDefaultL3NetworkToAnother(){
        L3NetworkInventory l31i = env.inventoryByName("l3-1")
        L3NetworkInventory l32i = env.inventoryByName("l3-2")
        L3NetworkInventory l3_3 = env.inventoryByName("l3-3")
        L3NetworkInventory l3_4 = env.inventoryByName("l3-4")
        VmInstanceInventory vmi = env.inventoryByName("vm")

        VmNicInventory nic1 = CollectionUtils.findOneOrNull(vmi.getVmNics(),
                { VmNicInventory it -> (it.getL3NetworkUuid() == l31i.getUuid()) })
        assert nic1 != null

        VmNicInventory nic2 = CollectionUtils.findOneOrNull(vmi.getVmNics(),
                { VmNicInventory it -> (it.getL3NetworkUuid() == l32i.getUuid()) })
        assert nic2 != null

        VmNicInventory nic3 = CollectionUtils.findOneOrNull(vmi.getVmNics(),
                { VmNicInventory it -> (it.getL3NetworkUuid() == l3_3.getUuid()) })
        assert nic3 != null

        VmNicInventory nic4 = CollectionUtils.findOneOrNull(vmi.getVmNics(),
                { VmNicInventory it -> (it.getL3NetworkUuid() == l3_4.getUuid()) })
        assert nic4 != null

        FlatDhcpBackend.ResetDefaultGatewayCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.RESET_DEFAULT_GATEWAY_PATH){ rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body,FlatDhcpBackend.ResetDefaultGatewayCmd.class)
            return rsp
        }
        updateVmInstance {
            uuid = vmi.uuid
            defaultL3NetworkUuid = l32i.uuid
        }

        TimeUnit.SECONDS.sleep(2);
        assert nic1.getMac() == cmd.macOfGatewayToRemove
        assert nic1.getGateway() == cmd.gatewayToRemove
        assert new BridgeNameFinder().findByL3Uuid(l31i.getUuid()) == cmd.bridgeNameOfGatewayToRemove

        assert nic2.getMac() == cmd.macOfGatewayToAdd
        assert nic2.getGateway() == cmd.gatewayToAdd
        assert new BridgeNameFinder().findByL3Uuid(l32i.getUuid()) == cmd.bridgeNameOfGatewayToAdd

        cmd = null
        updateVmInstance {
            uuid = vmi.uuid
            defaultL3NetworkUuid = l3_3.uuid
        }

        TimeUnit.SECONDS.sleep(2);
        assert nic2.getMac() == cmd.macOfGatewayToRemove
        assert nic2.getGateway() == cmd.gatewayToRemove
        assert new BridgeNameFinder().findByL3Uuid(l32i.getUuid()) == cmd.bridgeNameOfGatewayToRemove

        assert null == cmd.macOfGatewayToAdd
        assert null == cmd.gatewayToAdd
        assert null == cmd.bridgeNameOfGatewayToAdd

        cmd = null
        updateVmInstance {
            uuid = vmi.uuid
            defaultL3NetworkUuid = l3_4.uuid
        }

        TimeUnit.SECONDS.sleep(2);
        assert cmd == null

        updateVmInstance {
            uuid = vmi.uuid
            defaultL3NetworkUuid = l31i.uuid
        }

        TimeUnit.SECONDS.sleep(2);
        assert null == cmd.macOfGatewayToRemove
        assert null == cmd.gatewayToRemove
        assert null == cmd.bridgeNameOfGatewayToRemove

        assert nic1.getMac() == cmd.macOfGatewayToAdd
        assert nic1.getGateway() == cmd.gatewayToAdd
        assert new BridgeNameFinder().findByL3Uuid(l31i.getUuid()) == cmd.bridgeNameOfGatewayToAdd

    }
    @Override
    void clean() {
        env.delete()
    }
}
