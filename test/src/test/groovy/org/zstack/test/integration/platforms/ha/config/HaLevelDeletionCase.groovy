package org.zstack.test.integration.platforms.ha.config

import org.zstack.ha.header.VmHaLevel
import org.zstack.header.image.ImageVO
import org.zstack.image.ImageSystemTags
import org.zstack.sdk.GetVmInstanceHaLevelResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class HaLevelDeletionCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    L3NetworkInventory l3
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
            testHaTagValidation()
            testDeleteHaLevelWillDeleteInhibit()
        }
    }

    void prepare() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory
        image = env.inventoryByName("image1") as ImageInventory
    }

    void testHaTagValidation() {
        logger.info("Test 001: create a VM with ha::NeverStop, expect VmHa NeverStop")
        def neverStopVm = createVmInstance {
            delegate.name = "ha-never-stop"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "size"        : gb(10),
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = ["ha::NeverStop"]
        } as VmInstanceInventory

        def result001 = getVmInstanceHaLevel {
            delegate.uuid = neverStopVm.uuid
        } as GetVmInstanceHaLevelResult
        assert result001.level == VmHaLevel.NeverStop.toString()

        def records = zqlQuery("query VmHa where uuid = '${neverStopVm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()

        logger.info("Test 002: create a VM with ha::None, expect VmHa None")
        def noneVm = createVmInstance {
            delegate.name = "ha-none"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [l3.uuid]
            delegate.platform = "Linux"
            delegate.guestOsType = "Linux"
            delegate.architecture = "x86_64"
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "size"        : gb(10),
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64"
                ]
            ]
            delegate.systemTags = ["ha::None"]
        } as VmInstanceInventory

        def result002 = getVmInstanceHaLevel {
            delegate.uuid = noneVm.uuid
        } as GetVmInstanceHaLevelResult
        assert result002.level == null

        records = zqlQuery("query VmHa where uuid = '${noneVm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.None.toString()
    }

    void testDeleteHaLevelWillDeleteInhibit() {
        logger.info("Test 101: set NeverStop on a running VM, expect no inhibition")
        setVmInstanceHaLevel {
            delegate.uuid = vm.uuid
            delegate.level = VmHaLevel.NeverStop.toString()
        }

        def records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records.size() == 1
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == null

        logger.info("Test 102: destroy the VM, expect inhibition and the NeverStop level kept")
        destroyVmInstance {
            delegate.uuid = vm.uuid
        }

        records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before destroying VM"

        logger.info("Test 103: create and delete an unrelated image system tag, VmHa stays")
        def imageTag = createSystemTag {
            delegate.resourceType = ImageVO.class.simpleName
            delegate.resourceUuid = image.uuid
            delegate.tag = ImageSystemTags.IMAGE_INJECT_QEMUGA_TOKEN
        }
        assert querySystemTag {
            delegate.conditions = ["resourceUuid=${image.uuid}", "tag=${ImageSystemTags.IMAGE_INJECT_QEMUGA_TOKEN}"]
        }.size() == 1
        deleteTag {
            delegate.uuid = imageTag.uuid
        }
        assert querySystemTag {
            delegate.conditions = ["resourceUuid=${image.uuid}", "tag=${ImageSystemTags.IMAGE_INJECT_QEMUGA_TOKEN}"]
        }.size() == 0

        records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.NeverStop.toString()
        assert records[0].inhibitionReason == "Before destroying VM"

        logger.info("Test 104: delete the HA level, expect None and inhibition cleared")
        deleteVmInstanceHaLevel {
            delegate.uuid = vm.uuid
        }

        records = zqlQuery("query VmHa where uuid = '${vm.uuid}'")
        assert records[0].haLevel == VmHaLevel.None.toString()
        assert records[0].inhibitionReason == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
