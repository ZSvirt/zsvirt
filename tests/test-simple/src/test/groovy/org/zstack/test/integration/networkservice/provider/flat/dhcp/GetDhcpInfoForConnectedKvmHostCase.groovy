package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmSystemTags
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.GetVmHostnameResult
import org.zstack.sdk.HostInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.tag.SystemTagCreator
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

class GetDhcpInfoForConnectedKvmHostCase extends SubCase {

    EnvSpec env

    VmInstanceInventory vm
    L3NetworkInventory l3

    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
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

                attachBackupStorage("sftp")
            }

            vm{
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
            runTest()
        }
    }


    void runTest(){
        HostInventory host = env.inventoryByName("kvm")
        VmInstanceInventory vm = env.inventoryByName("vm")

        setVmHostname {
            uuid = vm.uuid
            hostname = vm.vmNics[0].ip.replaceAll("\\.", "--")
        }

        SystemTagCreator creator = VmSystemTags.MULTIPLE_GATEWAY.newSystemTagCreator(vm.uuid)
        creator.setTagByTokens(map(
                e(VmSystemTags.MULTIPLE_GATEWAY_TOKEN, true)
        ))
        creator.recreate = true
        creator.create()

        FlatDhcpBackend.BatchApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e ->
            cmd = JSONObjectUtil.toObject(e.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            return rsp
        }

        reconnectHost {
            uuid = host.uuid
        }
        assert null != cmd
        assert 1 == cmd.dhcpInfos.size()
        assert 1 == cmd.dhcpInfos.get(0).dhcp.size()
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.uuid, VmSystemTags.HOSTNAME_TOKEN)
        assert cmd.dhcpInfos.get(0).dhcp.get(0).vmMultiGateway

        GetVmHostnameResult result = getVmHostname {
            uuid = vm.uuid
        }
        assert result != null
        assert result.getHostname() == vm.vmNics[0].ip.replaceAll("\\.", "--")

        cmd = null
        deleteVmHostname {
            uuid = vm.uuid
        }
        assert VmSystemTags.HOSTNAME.getTokenByResourceUuid(vm.uuid, VmSystemTags.HOSTNAME_TOKEN) == null

        reconnectHost {
            uuid = host.uuid
        }
        assert null != cmd
        assert 1 == cmd.dhcpInfos.size()
        assert 1 == cmd.dhcpInfos.get(0).dhcp.size()
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == vm.vmNics[0].ip.replaceAll("\\.", "-")
    }

    @Override
    void clean() {
        env.delete()
    }
}
