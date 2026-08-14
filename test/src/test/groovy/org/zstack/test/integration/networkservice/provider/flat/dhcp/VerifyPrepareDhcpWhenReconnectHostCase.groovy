package org.zstack.test.integration.networkservice.provider.flat.dhcp

import org.springframework.http.HttpEntity
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.network.service.virtualrouter.vyos.VyosConstants
import org.zstack.sdk.HostInventory
import org.zstack.sdk.VirtualRouterVmInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.networkservice.provider.NetworkServiceProviderTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class VerifyPrepareDhcpWhenReconnectHostCase extends SubCase {
    EnvSpec env
    @Override
    void setup() {
        useSpring(NetworkServiceProviderTest.springSpec)
    }

    @Override
    void environment() {
        env = env{
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 2
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

                image {
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
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
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                l2NoVlanNetwork {
                    name = "l2-1"
                    physicalInterface = "eth0"

                    l3Network {
                        name = "l3-1"

                        service {
                            provider = FlatNetworkServiceConstant.FLAT_NETWORK_SERVICE_TYPE_STRING
                            types = [NetworkServiceType.DHCP.toString(), UserdataConstant.USERDATA_TYPE_STRING]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
                        }

                        service {
                            provider = VyosConstants.PROVIDER_TYPE
                            types = [EipConstant.EIP_NETWORK_SERVICE_TYPE,
                                     NetworkServiceType.DNS.toString(),
                                     NetworkServiceType.SNAT.toString()]
                        }

                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }

                    l3Network {
                        name = "pubL3-1"
                        category = "Public"

                        ip {
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }

                }

                virtualRouterOffering {
                    name = "vr-offering"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3-1")
                    usePublicL3Network("pubL3-1")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useImage("image")
                useDefaultL3Network("l3-1")
                useL3Networks("l3-1")
                useInstanceOffering("instanceOffering")
            }
        }
    }

    @Override
    void test() {
        env.create {
            checkDhcpWork()
        }
    }

    void checkDhcpWork(){
        def host = queryHost {}[0] as HostInventory
        def vm = env.inventoryByName("vm") as VmInstanceInventory

        setVmHostname {
            uuid = vm.uuid
            hostname = "test-name"
        }

        def called = 0
        FlatDhcpBackend.BatchApplyDhcpCmd cmd = null
        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { rsp, HttpEntity<String> e1 ->
            cmd = JSONObjectUtil.toObject(e1.body, FlatDhcpBackend.BatchApplyDhcpCmd.class)
            called += 1
            return rsp
        }

        stopVmInstance {
            uuid = vm.uuid
        }
        startVmInstance {
            uuid = vm.uuid
        }
        assert called == 1
        assert cmd.dhcpInfos.size() == 1
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == "test-name"

        called = 0
        cmd = null
        reconnectHost { uuid=host.uuid }
        assert called == 1
        assert cmd.dhcpInfos.get(0).dhcp.get(0).hostname == "test-name"

        def vr = queryVirtualRouterVm {}[0] as VirtualRouterVmInventory
        assert vr != null

        destroyVmInstance { uuid=vm.uuid }

        reconnectHost { uuid=host.uuid }
        assert called == 1
    }

    @Override
    void clean() {
        env.delete()
    }
}
