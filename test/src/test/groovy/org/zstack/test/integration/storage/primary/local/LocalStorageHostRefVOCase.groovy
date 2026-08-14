package org.zstack.test.integration.storage.primary.local

import org.springframework.http.HttpEntity
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.SimpleQuery
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.network.securitygroup.SecurityGroupConstant
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.sdk.CreateDataVolumeAction
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.storage.primary.local.LocalStorageHostRefVO_
import org.zstack.storage.primary.local.LocalStorageKvmBackend
import org.zstack.storage.primary.local.LocalStorageSystemTags
import org.zstack.testlib.DiskOfferingSpec
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.KVMHostSpec
import org.zstack.testlib.PrimaryStorageSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.Test
import org.zstack.utils.data.SizeUnit
import static org.zstack.utils.CollectionDSL.e
import static org.zstack.utils.CollectionDSL.map

/**
 * Created by lining on 2017/3/5.
 */
class LocalStorageHostRefVOCase extends SubCase{

    EnvSpec env

    DatabaseFacade dbf

    long volumeBitSize = SizeUnit.GIGABYTE.toByte(10)

    @Override
    void setup() {
        spring {
            sftpBackupStorage()
            localStorage()
            virtualRouter()
            securityGroup()
            kvm()
        }
    }

    @Override
    void environment() {
        env = Test.makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(8)
                cpu = 4
            }

            diskOffering {
                name = 'diskOffering'
                diskSize = volumeBitSize
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
                    attachPrimaryStorage("local_2")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }

                localPrimaryStorage {
                    name = "local_2"
                    url = "/local_ps_2"
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
            checkLocalStorageHostRefWhenCreateDiskFail()
        }
    }

    /**
     * Test Case: After the creation of cloud disk failure，LocalStorageHostRefVO.availableCapacity automatic recovery
     *
     * Test action:
     * 1.Build 2 PS, host
     * 2.Check LocalStorageHostRefVO available capacity
     * 3.Failed to create cloud disk
     * 4.Check primary storage available capacity
     * 5.Check LocalStorageHostRefVO available capacity
     */
    void checkLocalStorageHostRefWhenCreateDiskFail(){

        dbf = bean(DatabaseFacade.class)

        // 1.Build PS, host by default
        PrimaryStorageSpec primaryStorageSpec = env.specByName("local")
        PrimaryStorageSpec primaryStorageSpec2 = env.specByName("local_2")
        KVMHostSpec kvmHostSpec = env.specByName("kvm")


        // 2.Check LocalStorageHostRefVO available capacity
        checkLocalStorageHostRefVO(kvmHostSpec.inventory.uuid, primaryStorageSpec.inventory.uuid)
        checkLocalStorageHostRefVO(kvmHostSpec.inventory.uuid, primaryStorageSpec2.inventory.uuid)


        // 3.Failed to create cloud disk
        env.afterSimulator(LocalStorageKvmBackend.CREATE_EMPTY_VOLUME_PATH) { rsp, HttpEntity<String> e1 ->
            throw new Exception("")
        }

        String localStorageSystemTag = LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME.instantiateTag(
                map(e(LocalStorageSystemTags.DEST_HOST_FOR_CREATING_DATA_VOLUME_TOKEN, kvmHostSpec.inventory.uuid))
        )
        DiskOfferingSpec diskOfferingSpec = env.specByName("diskOffering")
        CreateDataVolumeAction action = new CreateDataVolumeAction(
                sessionId: Test.currentEnvSpec.session.uuid,
                primaryStorageUuid: primaryStorageSpec2.inventory.uuid,
                name: "dataVolume",
                systemTags:[localStorageSystemTag],
                diskOfferingUuid: diskOfferingSpec.inventory.uuid
        )
        CreateDataVolumeAction.Result createDataVolumeActionResult = action.call()
        assert null != createDataVolumeActionResult.error
        assert null == createDataVolumeActionResult.value


        // 4.Check primary storage available capacity
        PrimaryStorageInventory primaryStorageInventory =  queryPrimaryStorage {
            conditions=["uuid=${primaryStorageSpec.inventory.uuid}".toString()]
        }[0]
        assert primaryStorageInventory.totalCapacity == primaryStorageInventory.availableCapacity

        PrimaryStorageInventory primaryStorageInventory2 =  queryPrimaryStorage {
            conditions=["uuid=${primaryStorageSpec2.inventory.uuid}".toString()]
        }[0]
        assert primaryStorageInventory2.totalCapacity == primaryStorageInventory2.availableCapacity


        // 5.Check LocalStorageHostRefVO available capacity
        checkLocalStorageHostRefVO(kvmHostSpec.inventory.uuid, primaryStorageInventory.uuid)
        checkLocalStorageHostRefVO(kvmHostSpec.inventory.uuid, primaryStorageInventory2.uuid)

    }

    void checkLocalStorageHostRefVO(String huuid, String primaryStorageUuid){
        SimpleQuery<LocalStorageHostRefVO> hq = dbf.createQuery(LocalStorageHostRefVO.class)
        hq.add(LocalStorageHostRefVO_.hostUuid, SimpleQuery.Op.EQ, huuid)
        hq.add(LocalStorageHostRefVO_.primaryStorageUuid, SimpleQuery.Op.EQ, primaryStorageUuid)
        LocalStorageHostRefVO localStorageHostRefVO = hq.find()

        assert localStorageHostRefVO.availableCapacity == localStorageHostRefVO.totalCapacity
    }

    @Override
    void clean() {
        env.delete()
    }
}
