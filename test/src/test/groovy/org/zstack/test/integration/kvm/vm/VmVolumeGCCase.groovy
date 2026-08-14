package org.zstack.test.integration.kvm.vm

import org.springframework.http.HttpEntity
import org.zstack.compute.vm.DeleteVolumeGC
import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.gc.GarbageCollectorVO
import org.zstack.core.gc.GarbageCollectorVO_
import org.zstack.header.network.service.NetworkServiceType
import org.zstack.header.volume.DeleteVolumeMsg
import org.zstack.header.volume.DeleteVolumeReply
import org.zstack.header.volume.VolumeVO
import org.zstack.network.service.eip.EipConstant
import org.zstack.network.service.flat.FlatDhcpBackend
import org.zstack.network.service.flat.FlatNetworkServiceConstant
import org.zstack.network.service.userdata.UserdataConstant
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.test.integration.kvm.Env
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.*
import org.zstack.utils.data.SizeUnit

import static org.zstack.core.Platform.operr

/**
 * Created by yaoning.li on 2020/08/06.
 */
class VmVolumeGCCase extends SubCase {
    EnvSpec env

    DatabaseFacade dbf
    CloudBus bus

    @Override
    void setup() {
        useSpring(KvmTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
            }

            diskOffering {
                name = "diskOffering"
                diskSize = SizeUnit.GIGABYTE.toByte(1)
            }

            sftpBackupStorage {
                name = "sftp"
                url = "/sftp"
                username = "root"
                password = "password"
                hostname = "localhost"

                image {
                    name = "iso"
                    url  = "http://zstack.org/download/test.iso"
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
                        totalCpu = 1000
                        totalMem = SizeUnit.GIGABYTE.toByte(10000)
                    }

                    attachPrimaryStorage("local")
                    attachL2Network("l2")
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                    availableCapacity = SizeUnit.GIGABYTE.toByte(100000)
                    totalCapacity = SizeUnit.GIGABYTE.toByte(10000)
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
                            startIp = "192.168.0.2"
                            endIp = "192.168.255.0"
                            netmask = "255.255.0.0"
                            gateway = "192.168.0.1"
                        }
                    }
                }

                attachBackupStorage("sftp")
            }
        }
    }

    @Override
    void test() {
        dbf = bean(DatabaseFacade.class)
        bus = bean(CloudBus.class)

        env.create {
            testDeleteVolumeGC()
        }
    }

    void testDeleteVolumeGC() {
        InstanceOfferingInventory instanceOffering = env.inventoryByName("instanceOffering")
        L3NetworkInventory l3 = env.inventoryByName("l3")
        ImageInventory image = env.inventoryByName("iso")

        long expectVolumeNum = Q.New(VolumeVO.class).count()

        env.afterSimulator(FlatDhcpBackend.BATCH_APPLY_DHCP_PATH) { FlatDhcpBackend.ApplyDhcpRsp rsp, HttpEntity<java.lang.String> e ->
            rsp.setError("case mock error")
            return rsp
        }

        String rootVolumeUuid = null
        env.message(DeleteVolumeMsg.class) { DeleteVolumeMsg msg, CloudBus bus ->
            rootVolumeUuid = msg.getVolumeUuid()

            DeleteVolumeReply reply = new DeleteVolumeReply()
            reply.success = false
            reply.setError(operr("ase mock error"))
            bus.reply(msg, reply)
        }

        expect(AssertionError.class) {
            createVmInstance {
                name = "test-for-deleteVolumeGC"
                instanceOfferingUuid = instanceOffering.uuid
                imageUuid = image.uuid
                l3NetworkUuids = [l3.uuid]
            }
        }
        assert rootVolumeUuid != null

        env.revokeMessage(DeleteVolumeMsg.class, null)

        GarbageCollectorVO garbageCollectorVO = Q.New(GarbageCollectorVO.class)
            .eq(GarbageCollectorVO_.runnerClass, DeleteVolumeGC.class.name)
            .eq(GarbageCollectorVO_.name, "gc-volume-${rootVolumeUuid}".toString())
            .find()
        assert garbageCollectorVO != null

        triggerGCJob {
            uuid = garbageCollectorVO.uuid
        }
        retryInSecs {
            long volumeNum = Q.New(VolumeVO.class).count()
            assert expectVolumeNum == volumeNum
        }
    }

    @Override
    void clean() {
        env.delete()
    }
}