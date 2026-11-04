package org.zstack.test.integration.storage.snapshot

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.DatabaseFacade
import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotMsg
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotReply
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotDirection
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotScope
import org.zstack.header.storage.snapshot.VolumeSnapshotVO
import org.zstack.header.storage.snapshot.VolumeSnapshotVO_
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_
import org.zstack.sdk.CreateVolumeSnapshotGroupAction
import org.zstack.sdk.DiskOfferingInventory
import org.zstack.sdk.ImageInventory
import org.zstack.sdk.InstanceOfferingInventory
import org.zstack.sdk.L3NetworkInventory
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeInventory
import org.zstack.sdk.VolumeSnapshotGroupInventory
import org.zstack.sdk.VolumeSnapshotGroupTreeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class VolumeSnapshotGroupTreeLifecycleCase extends SubCase {
    EnvSpec env
    DiskOfferingInventory diskOffering
    ImageInventory image
    InstanceOfferingInventory instanceOffering
    L3NetworkInventory l3

    private Set<String> preparedSuccessVolumeUuids
    private Set<String> preparedExpectedVolumeUuids
    private List<org.zstack.header.storage.snapshot.VolumeSnapshotInventory> preparedSnapshots
    private Boolean preparedAllowPartialSuccess

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.nfsOneVmEnv()
    }

    @Override
    void test() {
        env.create {
            prepareEnv()
            testHistoricalRevertChangesCurrentGroup()
            testAllFailedCreationDoesNotPersistGroup()
            testPartialCreationPersistsIncompleteGroup()
            testPartialGroupsBuildStrictForest()
            testOrdinarySnapshotsDoNotHideSnapshotGroups()
            testChainDeleteMakesDescendantIncomplete()
            testCommitDeleteReparentsDescendantGroup()
            testPartialCommitDeleteKeepsGroupTree()
        }
    }

    private void testHistoricalRevertChangesCurrentGroup() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-historical-revert", 1)
        VolumeSnapshotGroupInventory historical = createCompleteSnapshotGroup(
                fixture, "historical", "ffffffffffff4fffbfffffffffffffff")
        VolumeSnapshotGroupInventory latest = createCompleteSnapshotGroup(
                fixture, "latest", "00000000000040008000000000000001")
        VolumeSnapshotGroupVO historicalVO = Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.uuid, historical.uuid).find()
        VolumeSnapshotGroupVO latestVO = Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.uuid, latest.uuid).find()
        latestVO.createDate = historicalVO.createDate
        bean(DatabaseFacade.class).update(latestVO)
        assertCurrentGroup(fixture.vm.uuid, latest.uuid)

        stopVmInstance {
            uuid = fixture.vm.uuid
        }
        revertVmFromSnapshotGroup {
            uuid = historical.uuid
        }
        assertCurrentGroup(fixture.vm.uuid, historical.uuid)

        createOrdinarySnapshots([fixture.dataVolumeUuids[0]], "after-historical-revert")
        assertCurrentGroup(fixture.vm.uuid, historical.uuid)

        revertVmFromSnapshotGroup {
            uuid = latest.uuid
        }
        assertCurrentGroup(fixture.vm.uuid, latest.uuid)
    }

    private void prepareEnv() {
        diskOffering = env.inventoryByName("diskOffering") as DiskOfferingInventory
        image = env.inventoryByName("image1") as ImageInventory
        instanceOffering = env.inventoryByName("instanceOffering") as InstanceOfferingInventory
        l3 = env.inventoryByName("l3") as L3NetworkInventory

        env.message(CreateVolumesSnapshotMsg.class) { CreateVolumesSnapshotMsg msg, CloudBus bus ->
            assert preparedSuccessVolumeUuids != null :
                    "snapshot-group reply was not prepared: rootVolumeUuid=${msg.volumeSnapshotJobs[0].volumeUuid}"
            assert (msg.volumeSnapshotJobs*.volumeUuid as Set) == preparedExpectedVolumeUuids :
                    "snapshot-group jobs differ from the VM disks: expected=${preparedExpectedVolumeUuids}, " +
                            "actual=${msg.volumeSnapshotJobs*.volumeUuid}"
            assert msg.allowPartialSuccess == preparedAllowPartialSuccess :
                    "unexpected partial-success policy: expected=${preparedAllowPartialSuccess}, " +
                            "actual=${msg.allowPartialSuccess}"

            CreateVolumesSnapshotReply reply = new CreateVolumesSnapshotReply()
            reply.inventories = preparedSnapshots
            reply.failedSnapshotJobs = msg.volumeSnapshotJobs.findAll {
                !preparedSuccessVolumeUuids.contains(it.volumeUuid)
            }
            if (!reply.failedSnapshotJobs.isEmpty()) {
                ErrorCode partialError = new ErrorCode("TEST.PARTIAL_SNAPSHOT_FAILURE",
                        "intentionally failed snapshot jobs")
                if (msg.allowPartialSuccess) {
                    reply.partialError = partialError
                } else {
                    reply.error = partialError
                }
            }
            preparedSuccessVolumeUuids = null
            preparedExpectedVolumeUuids = null
            preparedSnapshots = null
            preparedAllowPartialSuccess = null
            bus.reply(msg, reply)
        }
    }

    private void testAllFailedCreationDoesNotPersistGroup() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-all-failed", 1)
        long before = groupCount(fixture.vm.uuid)
        prepareSnapshotGroupReply(fixture, Collections.emptySet(), "all-failed")

        CreateVolumeSnapshotGroupAction.Result result = callCreateSnapshotGroup(fixture.vm, "all-failed")

        assert result.error != null : "all-failed snapshot group creation must return an error"
        assert result.error.opaque?.get("volume.snapshot.group.inventory") == null :
                "all-failed creation must not return a persisted snapshot group"
        assert groupCount(fixture.vm.uuid) == before :
                "all-failed creation must not persist a snapshot group"
        assert queryTree(fixture.vm.uuid).isEmpty() :
                "all-failed creation must not add a snapshot-group tree node"
        stopFixtureVm(fixture)
    }

    private void testPartialCreationPersistsIncompleteGroup() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-partial", 2)
        String rootVolumeUuid = fixture.vm.rootVolumeUuid
        prepareSnapshotGroupReply(fixture, [rootVolumeUuid] as Set, "partial")

        CreateVolumeSnapshotGroupAction.Result result = callCreateSnapshotGroup(fixture.vm, "partial")

        assert result.error != null : "partial snapshot group creation must return an error"
        assert result.error.opaque?.get("volume.snapshot.group.inventory") != null :
                "partial creation error must carry the persisted snapshot group"
        VolumeSnapshotGroupInventory group = JSONObjectUtil.rehashObject(
                result.error.opaque["volume.snapshot.group.inventory"], VolumeSnapshotGroupInventory.class)
        assert group.snapshotCount == 3
        assert group.volumeSnapshotRefs.size() == 3
        assert group.volumeSnapshotRefs.find { it.volumeUuid == rootVolumeUuid && !it.snapshotDeleted }
        def failedRefs = group.volumeSnapshotRefs.findAll { it.snapshotDeleted }
        assert failedRefs*.volumeUuid as Set == fixture.dataVolumeUuids as Set
        assert failedRefs*.volumeSnapshotUuid.toSet().size() == failedRefs.size() :
                "failed snapshot-group refs must use distinct placeholder snapshot UUIDs"
        assert Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, group.uuid).isExists()

        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(queryTree(fixture.vm.uuid))
        assert nodes.keySet() == [group.uuid] as Set
        assert nodes[group.uuid].incomplete
        assert nodes[group.uuid].refs.find { it.volumeUuid == rootVolumeUuid }.snapshot != null
        assert fixture.dataVolumeUuids.every { volumeUuid ->
            nodes[group.uuid].refs.find { it.volumeUuid == volumeUuid }.snapshot == null
        }
        stopFixtureVm(fixture)
    }

    private void testPartialGroupsBuildStrictForest() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-strict-forest", 2)
        String rootVolumeUuid = fixture.vm.rootVolumeUuid
        String dataVolume1Uuid = fixture.dataVolumeUuids[0]
        String dataVolume2Uuid = fixture.dataVolumeUuids[1]

        VolumeSnapshotGroupInventory group1 = createSnapshotGroup(
                fixture, "root-data-1", [rootVolumeUuid, dataVolume1Uuid] as Set)
        VolumeSnapshotGroupInventory group2 = createSnapshotGroup(
                fixture, "data-2", [dataVolume2Uuid] as Set)
        SQL.New(VolumeSnapshotGroupRefVO.class)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, group1.uuid)
                .eq(VolumeSnapshotGroupRefVO_.snapshotDeleted, true)
                .delete()
        SQL.New(VolumeSnapshotGroupRefVO.class)
                .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, group2.uuid)
                .eq(VolumeSnapshotGroupRefVO_.snapshotDeleted, true)
                .delete()
        assert flattenTrees(queryTree(fixture.vm.uuid)).values().every { !it.current } :
                "complementary incomplete snapshot groups must not be marked current"
        VolumeSnapshotGroupInventory group3 = createSnapshotGroup(
                fixture, "root-branch", [rootVolumeUuid] as Set)
        VolumeSnapshotGroupInventory group4 = createCompleteSnapshotGroup(fixture, "merge-all")

        List<VolumeSnapshotGroupTreeInventory> trees = queryTree(fixture.vm.uuid)
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(trees)
        assert nodes.keySet() == [group1.uuid, group2.uuid, group3.uuid, group4.uuid] as Set
        assert trees*.uuid as Set == [group1.uuid, group2.uuid] as Set
        assert nodes[group1.uuid].parentGroupUuids.isEmpty()
        assert nodes[group2.uuid].parentGroupUuids.isEmpty()
        assert nodes[group3.uuid].parentGroupUuids as Set == [group1.uuid] as Set
        assert nodes[group4.uuid].parentGroupUuids as Set ==
                [group1.uuid, group2.uuid, group3.uuid] as Set
        assert nodes[group3.uuid].parentGroupUuid == group1.uuid
        assert nodes[group4.uuid].parentGroupUuids.contains(nodes[group4.uuid].parentGroupUuid)
        assert nodes[group1.uuid].incomplete
        assert nodes[group2.uuid].incomplete
        assert nodes[group3.uuid].incomplete
        assert !nodes[group4.uuid].incomplete
        assertCurrentGroup(fixture.vm.uuid, group4.uuid)
        stopFixtureVm(fixture)
    }

    private void testOrdinarySnapshotsDoNotHideSnapshotGroups() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-with-single-snapshots", 4)
        List<String> allVolumeUuids = fixture.allVolumeUuids
        List<VolumeSnapshotGroupInventory> groups = []
        Set<String> ordinarySnapshotUuids = new LinkedHashSet<>()

        groups.add(createCompleteSnapshotGroup(fixture, "group-1"))
        ordinarySnapshotUuids.addAll(createOrdinarySnapshots(
                [fixture.dataVolumeUuids[1], fixture.dataVolumeUuids[2]], "between-1-2"))
        groups.add(createCompleteSnapshotGroup(fixture, "group-2"))
        ordinarySnapshotUuids.addAll(createOrdinarySnapshots(
                [fixture.dataVolumeUuids[0], fixture.dataVolumeUuids[2]], "between-2-3"))
        groups.add(createCompleteSnapshotGroup(fixture, "group-3"))
        ordinarySnapshotUuids.addAll(createOrdinarySnapshots(
                [fixture.dataVolumeUuids[3]], "between-3-4"))
        groups.add(createCompleteSnapshotGroup(fixture, "group-4"))

        List<String> stateBeforeQuery = snapshotGroupDbState(fixture.vm.uuid)
        List<VolumeSnapshotGroupTreeInventory> trees = queryTree(fixture.vm.uuid)
        queryTree(fixture.vm.uuid)
        assert snapshotGroupDbState(fixture.vm.uuid) == stateBeforeQuery :
                "snapshot-group tree query must not mutate group, ref or snapshot state"
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(trees)
        assert nodes.keySet() == groups*.uuid as Set :
                "tree API must return all snapshot groups and no ordinary-snapshot nodes"
        assert trees*.uuid as Set == [groups[0].uuid] as Set
        groups.eachWithIndex { VolumeSnapshotGroupInventory group, int index ->
            VolumeSnapshotGroupTreeInventory node = nodes[group.uuid]
            assert !node.incomplete
            assert node.refs.size() == allVolumeUuids.size()
            assert node.refs.every { it.snapshot != null }
            assert node.refs.every { !ordinarySnapshotUuids.contains(it.volumeSnapshotUuid) }
            assert node.current == (index == groups.size() - 1)
            if (index > 0) {
                assert node.parentGroupUuids as Set == [groups[index - 1].uuid] as Set
                assert node.parentGroupUuid == groups[index - 1].uuid
            }
        }
        ordinarySnapshotUuids.each { String snapshotUuid ->
            assert Q.New(VolumeSnapshotVO.class).eq(VolumeSnapshotVO_.uuid, snapshotUuid).isExists()
        }
        stopFixtureVm(fixture)
    }

    private void testCommitDeleteReparentsDescendantGroup() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-commit-delete", 1)
        VolumeSnapshotGroupInventory group1 = createCompleteSnapshotGroup(fixture, "base")
        VolumeSnapshotGroupInventory group2 = createCompleteSnapshotGroup(fixture, "middle")
        VolumeSnapshotGroupInventory group3 = createCompleteSnapshotGroup(fixture, "child")

        deleteVolumeSnapshotGroup {
            uuid = group2.uuid
            direction = DeleteVolumeSnapshotDirection.Commit.toString()
            scope = DeleteVolumeSnapshotScope.Single.toString()
        }

        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(queryTree(fixture.vm.uuid))
        assert nodes.keySet() == [group1.uuid, group3.uuid] as Set
        assert nodes[group3.uuid].parentGroupUuids as Set == [group1.uuid] as Set
        assert nodes[group3.uuid].parentGroupUuid == group1.uuid
        assert !nodes[group1.uuid].incomplete
        assert !nodes[group3.uuid].incomplete
        assertCurrentGroup(fixture.vm.uuid, group3.uuid)
        stopFixtureVm(fixture)
    }

    private void testChainDeleteMakesDescendantIncomplete() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-chain-delete", 2)
        VolumeSnapshotGroupInventory group1 = createCompleteSnapshotGroup(fixture, "base")
        VolumeSnapshotGroupInventory group2 = createSnapshotGroup(
                fixture, "data-2-branch", [fixture.dataVolumeUuids[1]] as Set)
        VolumeSnapshotGroupInventory group3 = createCompleteSnapshotGroup(fixture, "after-branch")

        deleteVolumeSnapshotGroup {
            uuid = group2.uuid
            direction = DeleteVolumeSnapshotDirection.Auto.toString()
            scope = DeleteVolumeSnapshotScope.Chain.toString()
        }
        assert !Q.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, group2.uuid).isExists()

        VolumeSnapshotGroupInventory group4 = createCompleteSnapshotGroup(fixture, "after-chain-delete")
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(queryTree(fixture.vm.uuid))
        assert nodes.keySet() == [group1.uuid, group3.uuid, group4.uuid] as Set
        assert nodes[group3.uuid].incomplete
        assert !nodes[group4.uuid].incomplete
        assert nodes[group3.uuid].refs.find {
            it.volumeUuid == fixture.dataVolumeUuids[1]
        }.snapshotDeleted
        assert nodes[group4.uuid].parentGroupUuids as Set == [group1.uuid, group3.uuid] as Set
        assert nodes[group4.uuid].parentGroupUuids.contains(nodes[group4.uuid].parentGroupUuid)
        stopFixtureVm(fixture)
    }

    private void testPartialCommitDeleteKeepsGroupTree() {
        VmFixture fixture = createVmWithDataVolumes("snapshot-group-partial-delete", 4)
        List<VolumeSnapshotGroupInventory> groups = (1..7).collect {
            createCompleteSnapshotGroup(fixture, "group-${it}")
        }
        List<String> targetVolumeUuids = fixture.allVolumeUuids

        (1..5).each { int groupIndex ->
            String volumeUuid = targetVolumeUuids[groupIndex - 1]
            String snapshotUuid = groups[groupIndex].volumeSnapshotRefs.find {
                it.volumeUuid == volumeUuid
            }.volumeSnapshotUuid
            deleteVolumeSnapshot {
                uuid = snapshotUuid
                direction = DeleteVolumeSnapshotDirection.Commit.toString()
                scope = DeleteVolumeSnapshotScope.Single.toString()
            }
        }

        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(queryTree(fixture.vm.uuid))
        assert nodes.keySet().containsAll(groups*.uuid)
        assert !nodes[groups[0].uuid].incomplete
        assert !nodes[groups[6].uuid].incomplete
        (1..5).each { int groupIndex ->
            String expectedDeletedVolumeUuid = targetVolumeUuids[groupIndex - 1]
            VolumeSnapshotGroupTreeInventory node = nodes[groups[groupIndex].uuid]
            assert node.incomplete
            assert node.refs.size() == targetVolumeUuids.size()
            def deletedRefs = node.refs.findAll { it.snapshotDeleted }
            assert deletedRefs.size() == 1
            assert deletedRefs[0].volumeUuid == expectedDeletedVolumeUuid
            assert deletedRefs[0].snapshot == null
            assert node.refs.findAll { !it.snapshotDeleted }.every { it.snapshot != null }
        }
        assert nodes[groups[6].uuid].current
        assert nodes[groups[6].uuid].parentGroupUuids.contains(groups[5].uuid)
        assert nodes[groups[6].uuid].parentGroupUuids.contains(groups[4].uuid)
        assert nodes[groups[6].uuid].parentGroupUuids.contains(nodes[groups[6].uuid].parentGroupUuid)
        stopFixtureVm(fixture)
    }

    private VmFixture createVmWithDataVolumes(String name, int dataVolumeCount) {
        VmInstanceInventory vm = createVmInstance {
            delegate.name = name
            imageUuid = image.uuid
            instanceOfferingUuid = instanceOffering.uuid
            l3NetworkUuids = [l3.uuid]
            defaultL3NetworkUuid = l3.uuid
        } as VmInstanceInventory
        List<String> dataVolumeUuids = []
        dataVolumeCount.times { int index ->
            VolumeInventory volume = createDataVolume {
                delegate.name = "${name}-data-${index + 1}"
                diskOfferingUuid = diskOffering.uuid
            } as VolumeInventory
            attachDataVolumeToVm {
                volumeUuid = volume.uuid
                vmInstanceUuid = vm.uuid
            }
            dataVolumeUuids.add(volume.uuid)
        }
        return new VmFixture(vm: vm, dataVolumeUuids: dataVolumeUuids)
    }

    private VolumeSnapshotGroupInventory createCompleteSnapshotGroup(
            VmFixture fixture, String name, String resourceUuid = null) {
        return createSnapshotGroup(fixture, name, fixture.allVolumeUuids as Set, resourceUuid)
    }

    private VolumeSnapshotGroupInventory createSnapshotGroup(
            VmFixture fixture, String name, Set<String> successVolumeUuids, String resourceUuid = null) {
        prepareSnapshotGroupReply(fixture, successVolumeUuids, name)
        CreateVolumeSnapshotGroupAction.Result result = callCreateSnapshotGroup(fixture.vm, name, resourceUuid)
        if (successVolumeUuids.size() == fixture.allVolumeUuids.size()) {
            assert result.error == null :
                    "complete snapshot group creation failed: name=${name}, error=${JSONObjectUtil.toJsonString(result.error)}"
            assert result.value?.inventory != null
            return result.value.inventory
        }

        assert !successVolumeUuids.isEmpty()
        assert result.error != null
        assert result.error.opaque?.get("volume.snapshot.group.inventory") != null
        return JSONObjectUtil.rehashObject(
                result.error.opaque["volume.snapshot.group.inventory"], VolumeSnapshotGroupInventory.class)
    }

    private CreateVolumeSnapshotGroupAction.Result callCreateSnapshotGroup(
            VmInstanceInventory vm, String name, String resourceUuid = null) {
        return new CreateVolumeSnapshotGroupAction(
                name: name,
                rootVolumeUuid: vm.rootVolumeUuid,
                resourceUuid: resourceUuid,
                sessionId: adminSession()).call()
    }

    private void stopFixtureVm(VmFixture fixture) {
        VmInstanceInventory latest = queryVmInstance {
            conditions = ["uuid=${fixture.vm.uuid}"]
        }[0] as VmInstanceInventory
        if (latest.state == "Running") {
            stopVmInstance {
                uuid = latest.uuid
            }
        }
    }

    private void prepareSnapshotGroupReply(VmFixture fixture, Set<String> successVolumeUuids, String name,
                                           boolean allowPartialSuccess = true) {
        assert preparedSuccessVolumeUuids == null : "previous snapshot-group reply was not consumed"
        assert fixture.allVolumeUuids.containsAll(successVolumeUuids)
        preparedSuccessVolumeUuids = new LinkedHashSet<>(successVolumeUuids)
        preparedExpectedVolumeUuids = fixture.allVolumeUuids as Set
        preparedAllowPartialSuccess = allowPartialSuccess
        preparedSnapshots = successVolumeUuids.collect { String volumeUuid ->
            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                delegate.name = "${name}-${volumeUuid}"
                delegate.volumeUuid = volumeUuid
            } as VolumeSnapshotInventory
            return JSONObjectUtil.rehashObject(snapshot,
                    org.zstack.header.storage.snapshot.VolumeSnapshotInventory.class)
        }
    }

    private List<String> createOrdinarySnapshots(List<String> volumeUuids, String name) {
        return volumeUuids.collect { String volumeUuid ->
            VolumeSnapshotInventory snapshot = createVolumeSnapshot {
                delegate.name = "${name}-${volumeUuid}"
                delegate.volumeUuid = volumeUuid
            } as VolumeSnapshotInventory
            return snapshot.uuid
        }
    }

    private long groupCount(String vmUuid) {
        return Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid)
                .count()
    }

    private void assertCurrentGroup(String vmUuid, String expectedGroupUuid) {
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(queryTree(vmUuid))
        assert nodes[expectedGroupUuid]?.current
        assert nodes.values().count { it.current } == 1
    }

    private List<String> snapshotGroupDbState(String vmUuid) {
        List<String> groupUuids = Q.New(VolumeSnapshotGroupVO.class)
                .eq(VolumeSnapshotGroupVO_.vmInstanceUuid, vmUuid)
                .select(VolumeSnapshotGroupVO_.uuid)
                .listValues()
                .sort()
        List<String> refStates = Q.New(VolumeSnapshotGroupRefVO.class)
                .in(VolumeSnapshotGroupRefVO_.volumeSnapshotGroupUuid, groupUuids)
                .list()
                .collect { "${it.volumeSnapshotGroupUuid}:${it.volumeSnapshotUuid}:${it.snapshotDeleted}" }
                .sort()
        List<String> snapshotStates = Q.New(VolumeSnapshotVO.class)
                .in(VolumeSnapshotVO_.volumeUuid, queryVmInstance {
                    conditions = ["uuid=${vmUuid}"]
                }[0].allVolumes*.uuid)
                .list()
                .collect { "${it.uuid}:${it.parentUuid}:${it.latest}" }
                .sort()
        return groupUuids + refStates + snapshotStates
    }

    private List<VolumeSnapshotGroupTreeInventory> queryTree(String vmUuid) {
        return getVolumeSnapshotGroupTree {
            vmInstanceUuid = vmUuid
        } as List<VolumeSnapshotGroupTreeInventory>
    }

    private Map<String, VolumeSnapshotGroupTreeInventory> flattenTrees(
            List<VolumeSnapshotGroupTreeInventory> trees) {
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = new LinkedHashMap<>()
        Deque<VolumeSnapshotGroupTreeInventory> pending = new ArrayDeque<>(trees)
        while (!pending.isEmpty()) {
            VolumeSnapshotGroupTreeInventory node = pending.removeFirst()
            assert !nodes.containsKey(node.uuid) : "snapshot group appears more than once: ${node.uuid}"
            nodes[node.uuid] = node
            if (node.children != null) {
                pending.addAll(node.children)
            }
        }
        return nodes
    }

    @Override
    void clean() {
        env.delete()
    }

    private static class VmFixture {
        VmInstanceInventory vm
        List<String> dataVolumeUuids

        List<String> getAllVolumeUuids() {
            return [vm.rootVolumeUuid] + dataVolumeUuids
        }
    }
}
