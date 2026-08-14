package org.zstack.test.integration.storage.primary.local.psdisable

import org.springframework.http.HttpEntity
import org.zstack.kvm.KVMConstant
import org.zstack.core.db.DatabaseFacade
import org.zstack.header.storage.primary.PrimaryStorageState
import org.zstack.header.storage.primary.PrimaryStorageStateEvent
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.header.image.ImageConstant
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.ImageSpec 
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.testlib.VmSpec
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.utils.data.SizeUnit

/**
 * Created by shengyan on 2017/4/14.
 *
 * Test Action:
 * 1. Create vm1 on host1.
 * 2. Create vm2 on host2.
 * 3. Attach and Detach iso on vm1.
 * 4. Attach and Detach iso on vm2.
 * 5. Disable PS. 
 * 6. Attach iso on vm1.
 */
class LocalStorageDisablePSAttachIsoOnMultiHostCase extends SubCase{
    EnvSpec env

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        //env = Env.localStorageOneVmEnvForPrimaryStorage()
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
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
                    url  = "http://zstack.org/download/test.qcow2"
                }

                image {
                    name = "test-iso"
                    mediaType = ImageConstant.ImageMediaType.ISO.toString()
                    url  = "http://zstack.org/download/test.iso"
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


                    kvm {
                        name = "kvm1"
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
                name = "test-vm1"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }
            vm {
                name = "test-vm2"
                useInstanceOffering("instanceOffering")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm1")
            }
        }
    }

    @Override
    void test() {
        env.create {
            testLocalStorageAttachIsoMultiHostPSDisabled()
        }
    }


    void testLocalStorageAttachIsoMultiHostPSDisabled() {
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        VmSpec vmSpec1 = env.specByName("test-vm1")
        String vmUuid1 = vmSpec1.inventory.uuid
        VmSpec vmSpec2 = env.specByName("test-vm2")
        String vmUuid2 = vmSpec2.inventory.uuid
        String imageUuid = (env.specByName("test-iso") as ImageSpec).inventory.uuid
        DatabaseFacade dbf = bean(DatabaseFacade.class)

        def attach_iso_path_is_invoked1 = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            attach_iso_path_is_invoked1 = true
            return rsp
        }
        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid1
            sessionId = currentEnvSpec.session.uuid
        }
        assert attach_iso_path_is_invoked1

        def detach_iso_path_is_invoked1 = false
        env.afterSimulator(KVMConstant.KVM_DETACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            detach_iso_path_is_invoked1 = true
            return rsp
        }
        detachIsoFromVmInstance {
            vmInstanceUuid = vmUuid1
            sessionId = currentEnvSpec.session.uuid
        }
        assert detach_iso_path_is_invoked1

        def attach_iso_path_is_invoked2 = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            attach_iso_path_is_invoked2 = true
            return rsp
        }
        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid2
            sessionId = currentEnvSpec.session.uuid
        }
        assert attach_iso_path_is_invoked2

        def detach_iso_path_is_invoked2 = false
        env.afterSimulator(KVMConstant.KVM_DETACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            detach_iso_path_is_invoked2 = true
            return rsp
        }
        detachIsoFromVmInstance {
            vmInstanceUuid = vmUuid2
            sessionId = currentEnvSpec.session.uuid
        }
        assert detach_iso_path_is_invoked2

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.disable.toString()
        }
        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Disabled


        attach_iso_path_is_invoked1 = false
        env.afterSimulator(KVMConstant.KVM_ATTACH_ISO_PATH) { rsp, HttpEntity<String> e ->
            attach_iso_path_is_invoked1 = true
            return rsp
        }

        attachIsoToVmInstance {
            isoUuid = imageUuid
            vmInstanceUuid = vmUuid1
            sessionId = currentEnvSpec.session.uuid
        }

        assert attach_iso_path_is_invoked1

        changePrimaryStorageState {
            uuid = primaryStorageSpec.inventory.uuid
            stateEvent = PrimaryStorageStateEvent.enable.toString()
        }

        assert dbf.findByUuid(primaryStorageSpec.inventory.uuid, PrimaryStorageVO.class).state == PrimaryStorageState.Enabled
    }

    @Override
    void clean() {
        env.delete()
    }
}
