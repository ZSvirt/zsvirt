package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.vm.VmInstanceState
import org.zstack.header.vm.VmInstanceVO
import org.zstack.kvm.KVMAgentCommands
import org.zstack.kvm.KVMConstant
import org.zstack.kvm.KVMGlobalConfig
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ApiException
import org.zstack.sdk.GetResourceConfigResult
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.KVMHostInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

class ChangeVmCpuQuotaCase extends SubCase{
    EnvSpec env
    VmInstanceInventory vm
    DatabaseFacade dbf

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
            VmGlobalConfig.NUMA.updateValue(true)
            vm = env.inventoryByName("vm") as VmInstanceInventory

            dbf = bean(DatabaseFacade.class)
            testOnlineChangeCpuQuota()
            testCreateVMWithCpuQuotaConfig()
            testConfigVmCpuQuotaOutOfRange()
            testFailToConfigVmCpuQuota()
        }

    }

    void testOnlineChangeCpuQuota() {
        boolean called = false
        env.afterSimulator(KVMConstant.KVM_VM_UPDATE_CPU_QUOTA_PATH) { KVMAgentCommands.UpdateVmCpuQuotaRsp rsp, HttpEntity<String> e ->
            called = true
            return rsp
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running
        updateResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
            value = 40000
        }

        def config = getResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
        } as GetResourceConfigResult

        assert called
        assert config.value == '40000'
        env.cleanSimulatorHandlers()
    }

    void testCreateVMWithCpuQuotaConfig() {
        def image = env.inventoryByName("image1")
        def l3 = env.inventoryByName("l3")
        def instance = env.inventoryByName("instanceOffering") as InstanceOfferingInventory

        def vm = createVmInstance {
            name = "cap"
            imageUuid = image.uuid
            l3NetworkUuids = [l3.uuid]
            instanceOfferingUuid = instance.uuid
            systemTags = ["resourceConfig::kvm::vm.cpu.quota::50000"]
        } as VmInstanceInventory

        startVmInstance {
            uuid = vm.uuid
        }

        def config = getResourceConfig {
            resourceUuid = vm.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.getName()
        } as GetResourceConfigResult

        assert config.value == "50000";
    }

    void testConfigVmCpuQuotaOutOfRange() {

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)
        assert vo.state == VmInstanceState.Running

        expect(AssertionError.class) {
            updateResourceConfig {
                resourceUuid = vo.uuid
                category = KVMGlobalConfig.CATEGORY
                name = KVMGlobalConfig.VM_CPU_QUOTA.name
                value = 9999
            }
        }

        updateResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
            value = 10000
        }


        updateResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
            value = 1000000
        }

        expect(AssertionError.class) {
            updateResourceConfig {
                resourceUuid = vo.uuid
                category = KVMGlobalConfig.CATEGORY
                name = KVMGlobalConfig.VM_CPU_QUOTA.name
                value = 1000001
            }
        }
    }

    void testFailToConfigVmCpuQuota() {
        boolean called = false
        env.afterSimulator(KVMConstant.KVM_VM_UPDATE_CPU_QUOTA_PATH) { KVMAgentCommands.UpdateVmCpuQuotaRsp rsp, HttpEntity<String> e ->
            called = true
            rsp.success = false
            rsp.error = "on purpose"
            return rsp
        }

        VmInstanceVO vo = dbFindByUuid(vm.uuid, VmInstanceVO.class)

        def beforeConfig = getResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
        } as GetResourceConfigResult

        expect(AssertionError.class) {
            updateResourceConfig {
                resourceUuid = vo.uuid
                category = KVMGlobalConfig.CATEGORY
                name = KVMGlobalConfig.VM_CPU_QUOTA.name
                value = 10000
            }
        }

        def afterConfig = getResourceConfig {
            resourceUuid = vo.uuid
            category = KVMGlobalConfig.CATEGORY
            name = KVMGlobalConfig.VM_CPU_QUOTA.name
        } as GetResourceConfigResult

        assert called
        assert afterConfig.value == beforeConfig.value
        env.cleanSimulatorHandlers()
    }
}
