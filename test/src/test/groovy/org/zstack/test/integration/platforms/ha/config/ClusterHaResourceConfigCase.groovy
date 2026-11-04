package org.zstack.test.integration.platforms.ha.config

import org.zstack.ha.header.HaGlobalConfig
import org.zstack.ha.header.VmHaLevel
import org.zstack.header.vm.VmCreationStrategy
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.GetResourceConfigResult
import org.zstack.sdk.GetVmInstanceHaLevelResult
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.test.integration.ZStackTest
import org.zstack.test.integration.platforms.ha.HaTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

class ClusterHaResourceConfigCase extends SubCase {
    EnvSpec env

    ClusterInventory cluster
    L3NetworkInventory pubL3
    ImageInventory image1

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
            testCreateVmWithoutHaTag()
            testCreateVmWithDataVolume()
        }
    }

    void prepare() {
        cluster = env.inventoryByName("cluster") as ClusterInventory
        pubL3 = env.inventoryByName("pubL3") as L3NetworkInventory
        image1 = env.inventoryByName("image1") as ImageInventory
    }

    void testCreateVmWithoutHaTag() {
        logger.info("Test 001: set cluster vm.ha.level to NeverStop, expect the resource config stored")
        updateResourceConfig {
            delegate.resourceUuid = cluster.uuid
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
            delegate.value = VmHaLevel.NeverStop.toString()
        }
        GetResourceConfigResult clusterHa = getResourceConfig {
            delegate.resourceUuid = cluster.uuid
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
        } as GetResourceConfigResult
        assert clusterHa.value == VmHaLevel.NeverStop.toString()

        logger.info("Test 002: create a stopped VM without an HA tag, expect NeverStop and inhibitHA")
        VmInstanceInventory vm1 = createVmInstance {
            delegate.name = "vm1"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
        assert vm1.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm1.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm1.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm1.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 1
        def rows1 = zqlQuery("query VmHa where uuid = '${vm1.uuid}'".toString())
        assert rows1.size() == 1
        assert rows1[0]["uuid"] == vm1.uuid
        assert rows1[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows1[0]["haLevelUpdateTime"]
        assert rows1[0]["inhibitionReason"]
        assert rows1[0]["inhibitionTime"]

        logger.info("Test 003: create a stopped VM on the cluster without an HA tag, expect NeverStop and inhibitHA")
        VmInstanceInventory vm2 = createVmInstance {
            delegate.name = "vm2"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.clusterUuid = cluster.uuid
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
        assert vm2.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm2.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm2.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm2.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 1
        def rows2 = zqlQuery("query VmHa where uuid = '${vm2.uuid}'".toString())
        assert rows2.size() == 1
        assert rows2[0]["uuid"] == vm2.uuid
        assert rows2[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows2[0]["haLevelUpdateTime"]
        assert rows2[0]["inhibitionReason"]
        assert rows2[0]["inhibitionTime"]

        logger.info("Test 004: create a stopped VM with ha::None, expect HA level None and no inhibitHA")
        VmInstanceInventory vm3 = createVmInstance {
            delegate.name = "vm3"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.systemTags = ["ha::None"]
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
        assert vm3.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm3.uuid}".toString(), "tag=ha::None"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm3.uuid
        } as GetVmInstanceHaLevelResult).level == null
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm3.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 0
        def rows3 = zqlQuery("query VmHa where uuid = '${vm3.uuid}'".toString())
        assert rows3.size() == 1
        assert rows3[0]["uuid"] == vm3.uuid
        assert rows3[0]["haLevel"] == VmHaLevel.None.toString()
        assert rows3[0]["haLevelUpdateTime"]
        assert rows3[0]["inhibitionReason"] == null

        logger.info("Test 005: create a VM with ha::NeverStop, expect NeverStop and no inhibitHA")
        VmInstanceInventory vm4 = createVmInstance {
            delegate.name = "vm4"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
        assert vm4.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm4.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm4.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm4.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 0
        def rows4 = zqlQuery("query VmHa where uuid = '${vm4.uuid}'".toString())
        assert rows4.size() == 1
        assert rows4[0]["uuid"] == vm4.uuid
        assert rows4[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows4[0]["haLevelUpdateTime"]
        assert rows4[0]["inhibitionReason"] == null
    }

    void testCreateVmWithDataVolume() {
        logger.info("Test 101: set cluster vm.ha.level to NeverStop again, expect the resource config stored")
        updateResourceConfig {
            delegate.resourceUuid = cluster.uuid
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
            delegate.value = VmHaLevel.NeverStop.toString()
        }
        GetResourceConfigResult clusterHa = getResourceConfig {
            delegate.resourceUuid = cluster.uuid
            delegate.category = HaGlobalConfig.CATEGORY
            delegate.name = HaGlobalConfig.VM_HA_LEVEL.name
        } as GetResourceConfigResult
        assert clusterHa.value == VmHaLevel.NeverStop.toString()

        logger.info("Test 102: create a stopped VM with a data volume and no HA tag, expect NeverStop and inhibitHA")
        VmInstanceInventory vm11 = createVmInstance {
            delegate.name = "vm11"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                    "templateUuid": image1.uuid,
                ],
                [
                    "templateUuid": image1.uuid,
                    "name"        : "data-volume-from-vm11",
                ]
            ]
        } as VmInstanceInventory
        assert vm11.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm11.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm11.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm11.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 1
        def rows11 = zqlQuery("query VmHa where uuid = '${vm11.uuid}'".toString())
        assert rows11.size() == 1
        assert rows11[0]["uuid"] == vm11.uuid
        assert rows11[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows11[0]["haLevelUpdateTime"]
        assert rows11[0]["inhibitionReason"]
        assert rows11[0]["inhibitionTime"]

        logger.info("Test 103: create a stopped VM with a data volume on the cluster, expect NeverStop and inhibitHA")
        VmInstanceInventory vm12 = createVmInstance {
            delegate.name = "vm12"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.clusterUuid = cluster.uuid
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                    "templateUuid": image1.uuid,
                ],
                [
                    "templateUuid": image1.uuid,
                    "name"        : "data-volume-from-vm12",
                ]
            ]
        } as VmInstanceInventory
        assert vm12.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm12.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm12.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm12.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 1
        def rows12 = zqlQuery("query VmHa where uuid = '${vm12.uuid}'".toString())
        assert rows12.size() == 1
        assert rows12[0]["uuid"] == vm12.uuid
        assert rows12[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows12[0]["haLevelUpdateTime"]
        assert rows12[0]["inhibitionReason"]
        assert rows12[0]["inhibitionTime"]

        logger.info("Test 104: create a stopped VM with ha::None, expect HA level None and no inhibitHA")
        VmInstanceInventory vm13 = createVmInstance {
            delegate.name = "vm13"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.strategy = VmCreationStrategy.CreateStopped.toString()
            delegate.systemTags = ["ha::None"]
            delegate.diskAOs = [
                [
                    "boot": true,
                    "size": gb(20),
                    "platform": "Linux",
                    "guestOsType": "Linux",
                    "architecture": "x86_64"
                ]
            ]
        } as VmInstanceInventory
        assert vm13.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm13.uuid}".toString(), "tag=ha::None"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm13.uuid
        } as GetVmInstanceHaLevelResult).level == null
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm13.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 0
        def rows13 = zqlQuery("query VmHa where uuid = '${vm13.uuid}'".toString())
        assert rows13.size() == 1
        assert rows13[0]["uuid"] == vm13.uuid
        assert rows13[0]["haLevel"] == VmHaLevel.None.toString()
        assert rows13[0]["haLevelUpdateTime"]
        assert rows13[0]["inhibitionReason"] == null

        logger.info("Test 105: create a VM with a data volume and ha::NeverStop, expect NeverStop and no inhibitHA")
        VmInstanceInventory vm14 = createVmInstance {
            delegate.name = "vm14"
            delegate.cpuNum = 1
            delegate.memorySize = gb(1)
            delegate.l3NetworkUuids = [pubL3.uuid]
            delegate.systemTags = ["ha::NeverStop"]
            delegate.diskAOs = [
                [
                    "boot"        : true,
                    "platform"    : "Linux",
                    "guestOsType" : "Linux",
                    "architecture": "x86_64",
                    "templateUuid": image1.uuid,
                ],
                [
                    "templateUuid": image1.uuid,
                    "name"        : "data-volume-from-vm14",
                ]
            ]
        } as VmInstanceInventory
        assert vm14.clusterUuid == cluster.uuid
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm14.uuid}".toString(), "tag=ha::NeverStop"]
        } as List).size() == 1
        assert (getVmInstanceHaLevel {
            delegate.uuid = vm14.uuid
        } as GetVmInstanceHaLevelResult).level == VmHaLevel.NeverStop.toString()
        assert (querySystemTag {
            delegate.conditions = ["resourceUuid=${vm14.uuid}".toString(), "tag=inhibitHA"]
        } as List).size() == 0
        def rows14 = zqlQuery("query VmHa where uuid = '${vm14.uuid}'".toString())
        assert rows14.size() == 1
        assert rows14[0]["uuid"] == vm14.uuid
        assert rows14[0]["haLevel"] == VmHaLevel.NeverStop.toString()
        assert rows14[0]["haLevelUpdateTime"]
        assert rows14[0]["inhibitionReason"] == null
    }

    @Override
    void clean() {
        env.delete()
    }
}
