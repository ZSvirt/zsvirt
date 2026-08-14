package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.VmGlobalConfig
import org.zstack.core.db.Q
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.storage.primary.ImageCacheShadowVO
import org.zstack.header.storage.primary.ImageCacheShadowVO_
import org.zstack.header.storage.primary.ImageCacheVO
import org.zstack.header.storage.primary.ImageCacheVO_
import org.zstack.header.vm.VmInstanceDeletionPolicyManager
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.ApplianceVmInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.storage.primary.PrimaryStorageGlobalConfig
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil
import org.zstack.utils.path.PathUtil

import java.util.concurrent.TimeUnit

/**
 * 1. Create a virtual machine in the local host store
 * 2. Delete the virtual machine
 * 3. Clean the imagecache
 *
 * create by le.jin on 2020.10.13
 */
class CleanImageCacheOnLocalPrimaryStorageCase extends SubCase{

    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
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
                    name = "vr"
                    url  = "http://zstack.org/download/vr.qcow2"
                }
            }

            zone {
                name = "zone"
                description = "test"

                localPrimaryStorage {
                    name = "local-ps"
                    url = "/local_ps"
                    totalCapacity = SizeUnit.GIGABYTE.toByte(100)
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100)
                }

                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"

                    kvm {
                        name = "kvm"
                        managementIp = "localhost"
                        username = "root"
                        password = "password"
                    }

                    attachPrimaryStorage("local-ps")
                    attachL2Network("l2")
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
                    name = "vr-instance"
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
                useCluster("cluster")
                useImage("image1")
                useL3Networks("l3")
                useRootDiskOffering("diskOffering")
                useHost("kvm")
            }

        }
    }

    @Override
    void test() {
        env.create {
            testDelete()
        }
    }

    void testDelete(){
        PrimaryStorageInventory localps = env.inventoryByName("local-ps")
        ImageInventory image1 = env.inventoryByName("image1")
        ImageInventory vrImage = env.inventoryByName("vr")

        ImageCacheVO c = Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid,image1.getUuid()).find()
        assert c != null

        def checked = false
        def cmdTemp
        env.hijackSimulator(LocalStorageKvmBackend.DELETE_BITS_PATH){rsp,HttpEntity<String> e ->
            LocalStorageKvmBackend.DeleteBitsCmd cmd = JSONObjectUtil.toObject(e.body, LocalStorageKvmBackend.DeleteBitsCmd.class)
            cmdTemp = cmd
            assert PathUtil.parentFolder(c.installUrl) != cmd.path
            checked = true
            return rsp
        }

        VmGlobalConfig.VM_DELETION_POLICY.updateValue(VmInstanceDeletionPolicyManager.VmInstanceDeletionPolicy.Direct.toString());
        VmInstanceInventory vm = env.inventoryByName("vm")
        destroyVmInstance {
            uuid = vm.uuid
        }

        cleanUpImageCacheOnPrimaryStorage {
                delegate.uuid = localps.uuid
                delegate.force = true
        }
        assert cmdTemp !=null
        assert checked

        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.updateValue(1)
        retryInSecs {
            assert Q.New(ImageCacheShadowVO.class).eq(ImageCacheShadowVO_.imageUuid, image1.getUuid()).find() == null
            assert Q.New(ImageCacheVO.class).eq(ImageCacheVO_.imageUuid, image1.getUuid()).find() == null
        }

        ApplianceVmInventory vr = queryApplianceVm {}[0] as ApplianceVmInventory
        destroyVmInstance {
            uuid = vr.uuid
        }
        retryInSecs {
            assert !Q.New(ImageCacheShadowVO.class)
                    .eq(ImageCacheShadowVO_.imageUuid, vrImage.getUuid())
                    .isExists()
            assert !Q.New(ImageCacheVO.class)
                    .eq(ImageCacheVO_.imageUuid, vrImage.getUuid())
                    .isExists()
        }
    }
}