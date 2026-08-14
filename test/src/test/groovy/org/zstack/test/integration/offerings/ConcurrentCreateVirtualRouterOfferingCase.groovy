package org.zstack.test.integration.offerings

import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * 1. create 100 zones
 * 2. create 100 virtual router offerings in those zones
 *
 * confirm:
 * all virtual router offerings are default to the zones
 */
class ConcurrentCreateVirtualRouterOfferingCase extends SubCase {
    EnvSpec env

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
        }
    }

    void create100DefaultVirtualRouterOffering() {
        int num = 100
        CountDownLatch latch = new CountDownLatch(num)

        BackupStorageInventory bs = env.inventoryByName("sftp")
        ImageInventory image = env.inventoryByName("vr")

        List<Closure> createOffering = []

        (1..num).each { i ->
            Thread.start {
                try {
                    ZoneInventory zone = createZone { name = "zone-${i}" }
                    attachBackupStorageToZone {
                        zoneUuid = zone.uuid
                        backupStorageUuid = bs.uuid
                    }

                    L2NetworkInventory l2 = createL2NoVlanNetwork {
                        name = "l2"
                        physicalInterface = "eth0"
                        zoneUuid = zone.uuid
                    }

                    L3NetworkInventory l3 = createL3Network {
                        name = "pubL3"
                        l2NetworkUuid = l2.uuid
                    }

                    synchronized (createOffering) {
                        createOffering.add({
                            createVirtualRouterOffering {
                                name = "vr"
                                memorySize = SizeUnit.MEGABYTE.toByte(512)
                                cpuNum = 2
                                zoneUuid = zone.uuid
                                managementNetworkUuid = l3.uuid
                                publicNetworkUuid = l3.uuid
                                imageUuid = image.uuid
                            }
                        })
                    }
                } finally {
                    latch.countDown()
                }
            }
        }

        assert latch.await(5L, TimeUnit.MINUTES)

        latch = new CountDownLatch(createOffering.size())
        createOffering.each { c ->
            Thread.start {
                try {
                    c()
                } finally {
                    latch.countDown()
                }
            }
        }

        assert latch.await(5L, TimeUnit.MINUTES)

        List<VirtualRouterOfferingInventory> offerings = queryVirtualRouterOffering { conditions = [] }
        assert offerings.size() == createOffering.size()
        offerings.each {
            assert it.isDefault : JSONObjectUtil.toJsonString(it)
        }
    }

    @Override
    void test() {
        env.create {
            create100DefaultVirtualRouterOffering()
        }
    }
}
