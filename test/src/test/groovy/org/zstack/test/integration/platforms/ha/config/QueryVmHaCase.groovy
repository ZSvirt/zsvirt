package org.zstack.test.integration.platforms.ha.config

import org.zstack.ha.header.VmHaLevel
import org.zstack.sdk.HostInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatterBuilder
import java.util.Locale

class QueryVmHaCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3
    HostInventory kvm1
    ImageInventory image

    @Override
    void setup() {
        useSpring(ZStackTest.springSpec)
    }

    @Override
    void environment() {
        env = HaTest.normal()
    }

    @Override
    void test() {
        env.create {
            prepare()
            testHaZql()
        }
    }

    void prepare() {
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        kvm1 = env.inventoryByName("kvm1") as HostInventory
        image = env.inventoryByName("image1") as ImageInventory
    }

    void testHaZql() {
        logger.info("Test 001: create a VM and set NeverStop, expect VmHa fields from zql")
        def vm1 = createVmInstance {
            delegate.name = "x86-vm"
            delegate.clusterUuid = kvm1.clusterUuid
            delegate.hostUuid = kvm1.uuid
            delegate.imageUuid = image.uuid
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory

        setVmInstanceHaLevel {
            delegate.uuid = vm1.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }

        def now = LocalDateTime.now()
        def formatter = new DateTimeFormatterBuilder()
                .parseCaseInsensitive()
                .appendPattern("MMM d, yyyy h:mm:ss a")
                .toFormatter(Locale.ENGLISH)
        def results1 = zqlQuery("query VmHa where uuid = '${vm1.uuid}'")
        assert results1.size() == 1
        assert results1[0].uuid == vm1.uuid
        assert results1[0].haLevel == VmHaLevel.NeverStop.toString()
        assert results1[0].haLevelUpdateTime
        def haLevelUpdateTime1 = LocalDateTime.parse(results1[0].haLevelUpdateTime as String, formatter)
        assert Duration.between(haLevelUpdateTime1, now).abs().seconds < 2
        assert results1[0].inhibitionReason == null
        assert results1[0].inhibitionTime

        logger.info("Test 002: stop the VM with stopHA, expect NeverStop kept and inhibition recorded")
        stopVmInstance {
            delegate.uuid = vm1.uuid
            delegate.stopHA = "true"
        }

        now = LocalDateTime.now()
        def results2 = zqlQuery("query VmHa where uuid = '${vm1.uuid}'")
        assert results2.size() == 1
        assert results2[0].uuid == vm1.uuid
        assert results2[0].haLevel == VmHaLevel.NeverStop.toString()
        def haLevelUpdateTime2 = LocalDateTime.parse(results2[0].haLevelUpdateTime as String, formatter)
        assert haLevelUpdateTime2 == haLevelUpdateTime1
        assert results2[0].inhibitionReason == "Before VM stopping on hypervisor"
        assert results2[0].inhibitionTime
        def inhibitionTime = LocalDateTime.parse(results2[0].inhibitionTime as String, formatter)
        assert Duration.between(inhibitionTime, now).abs().seconds < 2

        def stopped = queryVmInstance {
            delegate.conditions = ["uuid=${vm1.uuid}"]
        }
        assert stopped.size() == 1
        assert stopped[0].state == "Stopped"
    }

    @Override
    void clean() {
        env.delete()
    }
}
