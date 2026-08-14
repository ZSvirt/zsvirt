package org.zstack.test.integration.network.l3network.ipv6

import org.zstack.header.image.ImageConstant
import org.zstack.sdk.*
import org.zstack.test.integration.kvm.KvmTest
import org.zstack.test.integration.network.NetworkTest
import org.zstack.test.integration.network.l3network.Env
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase

import java.util.stream.Collectors

/**
 * Created by shixin on 2018/10/16.
 */
class GetCandidateZonesClustersHostsForCreatingVmCase extends SubCase {
    EnvSpec env
    L3NetworkInventory l3_statefull
    L3NetworkInventory l3
    InstanceOfferingInventory offering
    DiskOfferingInventory diskOffering
    ImageInventory image
    HostInventory h1
    HostInventory h2
    BackupStorageInventory bs
    BackupStorageInventory bs2
    ZoneInventory zone
    PrimaryStorageInventory ps

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(NetworkTest.springSpec)
        useSpring(KvmTest.springSpec)

    }

    @Override
    void environment() {
        env = Env.Ipv6FlatL3Network()
    }

    @Override
    void test() {
        env.create {
            prepareEnv()
            testGetCandidateZonesClustersHostsForCreatingVm()
            testGetCandidatePrimaryStoragesAndClustersHostsForCreatingVmMsg()
            testGetCandidatePrimaryStoragesWhenAssignDataDisksForCreatingVmMsg()
        }
    }

    void prepareEnv() {
        l3_statefull = env.inventoryByName("l3-Statefull-DHCP")
        l3 = env.inventoryByName("l3")
        offering = env.inventoryByName("instanceOffering")
        diskOffering = env.inventoryByName("diskOffering")
        image = env.inventoryByName("image1")
        h1 = env.inventoryByName("kvm-1")
        h2 = env.inventoryByName("kvm-2")
        bs = env.inventoryByName("sftp")
        bs2 = env.inventoryByName("sftp2")
        zone = env.inventoryByName("zone")
        ps = env.inventoryByName("nfs")
    }

    void testGetCandidatePrimaryStoragesWhenAssignDataDisksForCreatingVmMsg() {
        GetCandidatePrimaryStoragesForCreatingVmResult getPsSuccessres = getCandidatePrimaryStoragesForCreatingVm {
            sessionId = adminSession()
            zoneUuid = zone.uuid
            l3NetworkUuids = [l3.uuid]
            imageUuid = image.uuid
            dataDiskSizes = [1024 * 1024 * 1024]
        }
        assert getPsSuccessres.rootVolumePrimaryStorages.size() == 1
        assert getPsSuccessres.dataVolumePrimaryStorages.size() == 1
    }

    void testGetCandidatePrimaryStoragesAndClustersHostsForCreatingVmMsg() {
        ImageInventory iso = addImage {
            name = "iso-image"
            url = "http://my-site/image.iso"
            backupStorageUuids = [bs.uuid, bs2.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        //test GetCandidateZonesClustersHostsForCreatingVm
        GetCandidateZonesClustersHostsForCreatingVmAction failAction = new GetCandidateZonesClustersHostsForCreatingVmAction()
        failAction.sessionId = adminSession()
        failAction.zoneUuid = zone.uuid
        failAction.imageUuid = iso.uuid
        failAction.l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        failAction.cpuNum = offering.cpuNum
        failAction.memorySize = offering.memorySize
        GetCandidateZonesClustersHostsForCreatingVmAction.Result failRes = failAction.call()
        assert failRes.error != null

        GetCandidateZonesClustersHostsForCreatingVmResult successres = getCandidateZonesClustersHostsForCreatingVm {
            zoneUuid = zone.uuid
            rootDiskSize = diskOffering.diskSize
            instanceOfferingUuid = offering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        }
        assert successres.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h1.uuid)
        assert successres.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h2.uuid)
        assert successres.zones.size() == 1
        assert successres.clusters.size() == 1

        //test GetCandidatePrimaryStoragesForCreatingVm
        GetCandidatePrimaryStoragesForCreatingVmAction getPsAction = new GetCandidatePrimaryStoragesForCreatingVmAction(
                l3NetworkUuids: [l3.uuid],
                imageUuid: iso.uuid,
                dataDiskOfferingUuids: [diskOffering.uuid],
                sessionId: adminSession()
        )
        GetCandidatePrimaryStoragesForCreatingVmAction.Result getPsResult = getPsAction.call()
        assert getPsResult.error != null

        GetCandidatePrimaryStoragesForCreatingVmResult getPsSuccessres = getCandidatePrimaryStoragesForCreatingVm {
            zoneUuid = zone.uuid
            rootDiskSize = diskOffering.diskSize
            dataDiskOfferingUuids = [diskOffering.uuid]
            imageUuid = iso.uuid
            l3NetworkUuids = [l3.uuid]
        }
        assert getPsSuccessres.rootVolumePrimaryStorages.stream().map { psInv -> psInv.getUuid() }.collect(Collectors.toList()).contains(ps.uuid)

        deleteImage {
            uuid = iso.uuid
        }
    }

    void testGetCandidateZonesClustersHostsForCreatingVm() {

        ImageInventory iso = addImage {
            name = "sized-image"
            url = "http://my-site/foo.iso"
            backupStorageUuids = [bs.uuid, bs2.uuid]
            format = ImageConstant.ISO_FORMAT_STRING
        }

        expect(AssertionError.class) {
            getCandidateZonesClustersHostsForCreatingVm {
                instanceOfferingUuid = offering.uuid
                imageUuid = iso.uuid
                l3NetworkUuids = [l3_statefull.uuid]
            }
        }

        expect(AssertionError.class) {
            getCandidateZonesClustersHostsForCreatingVm {
                rootDiskOfferingUuid = diskOffering.uuid
                instanceOfferingUuid = offering.uuid
                imageUuid = iso.uuid
                l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
            }
        }

        GetCandidateZonesClustersHostsForCreatingVmResult res = getCandidateZonesClustersHostsForCreatingVm {
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid]
        }
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        res = getCandidateZonesClustersHostsForCreatingVm {
            instanceOfferingUuid = offering.uuid
            imageUuid = image.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        }
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        res = getCandidateZonesClustersHostsForCreatingVm {
            zoneUuid = zone.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            instanceOfferingUuid = offering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
        }
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        expect(AssertionError.class) {
            res = getCandidateZonesClustersHostsForCreatingVm {
                zoneUuid = zone.uuid
                rootDiskOfferingUuid = diskOffering.uuid
                imageUuid = iso.uuid
                l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
            }
        }

        getCandidateZonesClustersHostsForCreatingVm {
            zoneUuid = zone.uuid
            rootDiskOfferingUuid = diskOffering.uuid
            imageUuid = iso.uuid
            l3NetworkUuids = [l3_statefull.uuid, l3.uuid]
            cpuNum = offering.cpuNum
            memorySize = offering.memorySize
        }

        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h1.uuid)
        assert res.hosts.stream().map { h -> h.getUuid() }.collect(Collectors.toList()).contains(h2.uuid)
        assert res.zones.size() == 1
        assert res.clusters.size() == 1

        deleteImage {
            uuid = iso.uuid
        }
    }
}
