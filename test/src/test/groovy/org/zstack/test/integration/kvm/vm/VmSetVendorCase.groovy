package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMSystemTags
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.resourceconfig.ResourceConfigFacade
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit

import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

class VmSetVendorCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    ResourceConfigFacade rcf
    def cpuModelName = "hygon"

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(2)
                cpu = 1
            }

            instanceOffering {
                name = "instanceOffering2"
                memory = SizeUnit.GIGABYTE.toByte(4)
                cpu = 2
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(20)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "image1"
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
                        systemTags = [
                                KVMSystemTags.CPU_MODEL_NAME.instantiateTag(map(e(KVMSystemTags.CPU_MODEL_NAME_TOKEN, cpuModelName)))
                        ]
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
                            provider = VirtualRouterConstant.PROVIDER_TYPE
                            types = [NetworkServiceType.DHCP.toString(), NetworkServiceType.DNS.toString()]
                        }

                        service {
                            provider = SecurityGroupConstant.SECURITY_GROUP_PROVIDER_TYPE
                            types = [SecurityGroupConstant.SECURITY_GROUP_NETWORK_SERVICE_TYPE]
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
                            startIp = "12.16.10.10"
                            endIp = "12.16.10.100"
                            netmask = "255.255.255.0"
                            gateway = "12.16.10.1"
                        }
                    }
                }

                virtualRouterOffering {
                    name = "vr"
                    memory = SizeUnit.MEGABYTE.toByte(512)
                    cpu = 2
                    useManagementL3Network("pubL3")
                    usePublicL3Network("pubL3")
                    useImage("vr")
                }

                attachBackupStorage("sftp")
            }

            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
            }
        }
    }

    @Override
    void test() {
        env.create {
            rcf = bean(ResourceConfigFacade.class)
            vm = env.inventoryByName("vm") as VmInstanceInventory

            testAutoSetVmCpuVendorOnHygonHost()
        }
    }

    void testAutoSetVmCpuVendorOnHygonHost() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering")

        def vm1 = createVmInstance {
            name = "vm1"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
        }

        assert rcf.getResourceConfigValue(VmGlobalConfig.VM_CPUID_VENDOR, vm1.uuid, String.class) == "AuthenticAMD"

        updateResourceConfig {
            name = VmGlobalConfig.VM_CPUID_VENDOR.name
            category = VmGlobalConfig.CATEGORY
            value = "None"
            resourceUuid = vm1.uuid
        }

        rebootVmInstance {
            uuid = vm1.uuid
        }

        assert rcf.getResourceConfigValue(VmGlobalConfig.VM_CPUID_VENDOR, vm1.uuid, String.class) == "None"

        destroyVmInstance {
            uuid = vm1.uuid
        }

        expungeVmInstance {
            uuid = vm1.uuid
        }
    }
}
