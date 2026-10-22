package org.zstack.test.integration.storage.snapshot

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.db.SQL
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.AccountConstant
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotMsg
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotReply
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupRefVO_
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO
import org.zstack.header.storage.snapshot.group.VolumeSnapshotGroupVO_
import org.zstack.identity.Account
import org.zstack.sdk.*
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.testlib.premium.PremiumEnv
import org.zstack.utils.data.SizeUnit
import org.zstack.utils.gson.JSONObjectUtil

/** Query and creation access on the same admin-owned snapshot group; only batch snapshot creation is a fixture. */
class VolumeSnapshotGroupQueryAccessCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    VolumeInventory dataVolume
    VolumeSnapshotGroupInventory group
    List<VolumeSnapshotInventory> snapshots
    AccountInventory vmOwnerAccount
    AccountInventory unrelated
    SessionInventory ownerSession
    SessionInventory unrelatedSession
    SessionInventory noVmRoleSession
    String l3Uuid
    List<VolumeSnapshotGroupInventory> groups = []
    List<String> createdVmUuids = []
    int creationIndex

    @Override
    void setup() {
        useSpring(PremiumEnv.makeSpring())
    }

    @Override
    void environment() {
        env = makeEnv {
            instanceOffering {
                name = "instanceOffering"
                memory = SizeUnit.GIGABYTE.toByte(1)
                cpu = 1
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
                    name = "image"
                    url = "http://zstack.org/download/test.qcow2"
                }
            }
            zone {
                name = "zone"
                cluster {
                    name = "cluster"
                    hypervisorType = "KVM"
                    kvm {
                        name = "kvm"
                        managementIp = "127.0.0.1"
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
                        ip {
                            startIp = "192.168.100.10"
                            endIp = "192.168.100.100"
                            netmask = "255.255.255.0"
                            gateway = "192.168.100.1"
                        }
                    }
                }
                attachBackupStorage("sftp")
            }
            vm {
                name = "vm"
                useInstanceOffering("instanceOffering")
                useImage("image")
                useL3Networks("l3")
                useDiskOfferings("diskOffering")
                useHost("kvm")
            }
        }
    }

    @Override
    void test() {
        env.create {
            prepareAdminGroupOnOwnedVm()
            assertReadable(ownerSession.uuid)
            assertReadable(adminSession())
            assertInvisible(unrelatedSession.uuid)
            testVmCreationAccess()
            testSharedVmDoesNotGrantDetailAccess()
            testDirectSnapshotSharing()
            testDetachedDataVolumeRemainsReadable()
            testStandaloneSnapshotKeepsOriginalAccess()
            testVmTransferMovesInheritedAccess()
        }
    }

    private void prepareAdminGroupOnOwnedVm() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        dataVolume = (queryVolume {
            conditions = ["vmInstanceUuid=${vm.uuid}".toString(), "type=Data"]
        } as List<VolumeInventory>)[0]
        vmOwnerAccount = accountWithRoles("snapshot-query-owner")
        unrelated = accountWithRoles("snapshot-query-unrelated")
        ownerSession = login(vmOwnerAccount)
        unrelatedSession = login(unrelated)
        AccountInventory noVmRoleAccount = accountWithRoles("snapshot-create-no-role", "volume", "snapshot")
        noVmRoleSession = login(noVmRoleAccount)
        l3Uuid = (env.inventoryByName("l3") as L3NetworkInventory).uuid
        shareResource {
            resourceUuids = [l3Uuid]
            accountUuids = [vmOwnerAccount.uuid, unrelated.uuid, noVmRoleAccount.uuid]
        }
        changeResourceOwner {
            resourceUuid = vm.uuid
            accountUuid = vmOwnerAccount.uuid
        }
        // VM ownership transfer also transfers its attached volumes.
        assert Account.getAccountUuidOfResource(vm.uuid) == vmOwnerAccount.uuid
        assert Account.getAccountUuidOfResource(vm.rootVolumeUuid) == vmOwnerAccount.uuid
        assert Account.getAccountUuidOfResource(dataVolume.uuid) == vmOwnerAccount.uuid

        snapshots = [vm.rootVolumeUuid, dataVolume.uuid].collect { String volume ->
            createVolumeSnapshot {
                name = "admin-snapshot-${volume}"
                volumeUuid = volume
                sessionId = adminSession()
            } as VolumeSnapshotInventory
        }
        snapshots.each { assert Account.getAccountUuidOfResource(it.uuid) == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID }
        List<org.zstack.header.storage.snapshot.VolumeSnapshotInventory> fixture = snapshots.collect {
            JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(it), org.zstack.header.storage.snapshot.VolumeSnapshotInventory.class)
        }
        int batchCalls = 0
        env.message(CreateVolumesSnapshotMsg.class) { CreateVolumesSnapshotMsg msg, CloudBus bus ->
            batchCalls++
            assert msg.volumeSnapshotJobs.collect { it.volumeUuid }.toSet() == [vm.rootVolumeUuid, dataVolume.uuid].toSet()
            CreateVolumesSnapshotReply reply = new CreateVolumesSnapshotReply()
            reply.inventories = fixture
            bus.reply(msg, reply)
        }
        group = createVolumeSnapshotGroup {
            name = "admin-group-on-owned-vm"
            rootVolumeUuid = vm.rootVolumeUuid
            sessionId = adminSession()
        } as VolumeSnapshotGroupInventory
        groups.add(group)
        assert batchCalls == 1
        assert group.snapshotCount == 2
        assert Account.getAccountUuidOfResource(group.uuid) == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
    }

    private void assertReadable(String session) {
        def queryGroup = new QueryVolumeSnapshotGroupAction(conditions: ["uuid=${group.uuid}".toString()],
                replyWithCount: true, sessionId: session).call()
        assert queryGroup.error == null
        assert queryGroup.value.total == 1
        assert queryGroup.value.inventories.collect { it.uuid } == [group.uuid]
        ZQLQueryReturn zqlGroup = zql("query volumeSnapshotGroup where uuid='${group.uuid}' return with (total)", session)
        assert zqlGroup.total == 1
        assert zqlGroup.inventories.collect { it.uuid } == [group.uuid]

        Set expected = snapshots.collect { it.uuid }.toSet()
        def queryMembers = new QueryVolumeSnapshotAction(conditions: ["groupUuid=${group.uuid}".toString()],
                replyWithCount: true, sessionId: session).call()
        assert queryMembers.error == null
        assert queryMembers.value.total == 2
        assert queryMembers.value.inventories.collect { it.uuid }.toSet() == expected
        ZQLQueryReturn adminMembers = zql("query volumeSnapshot where groupUuid='${group.uuid}' return with (total)", adminSession())
        ZQLQueryReturn members = zql("query volumeSnapshot where groupUuid='${group.uuid}' return with (total)", session)
        assert members.total == 2
        assert members.inventories.collect { it.uuid }.toSet() == expected
        assert members.inventories.sum { it.size as long } == adminMembers.inventories.sum { it.size as long }
        def page = new QueryVolumeSnapshotAction(conditions: ["groupUuid=${group.uuid}".toString()],
                replyWithCount: true, limit: 1, start: 1, sessionId: session).call()
        assert page.error == null
        assert page.value.total == 2
        assert page.value.inventories.size() == 1
        assert expected.contains(page.value.inventories[0].uuid)
        snapshots.each {
            ZQLQueryReturn detail = zql("query volumeSnapshot where uuid='${it.uuid}' return with (total)", session)
            assert detail.total == 1
            assert detail.inventories[0].uuid == it.uuid
            def size = new GetVolumeSnapshotSizeAction(uuid: it.uuid, sessionId: session).call()
            assert size.error == null
            assert size.value != null
        }
    }

    private void assertInvisible(String session) {
        def groups = new QueryVolumeSnapshotGroupAction(conditions: ["uuid=${group.uuid}".toString()],
                replyWithCount: true, sessionId: session).call()
        assert groups.error == null
        assert groups.value.total == 0
        assert groups.value.inventories.isEmpty()
        ZQLQueryReturn zqlGroups = zql("query volumeSnapshotGroup where uuid='${group.uuid}' return with (total)", session)
        assert zqlGroups.total == 0
        assert zqlGroups.inventories.isEmpty()
        def members = new QueryVolumeSnapshotAction(conditions: ["groupUuid=${group.uuid}".toString()],
                replyWithCount: true, sessionId: session).call()
        assert members.error == null
        assert members.value.total == 0
        assert members.value.inventories.isEmpty()
        ZQLQueryReturn zqlMembers = zql("query volumeSnapshot where groupUuid='${group.uuid}' return with (total)", session)
        assert zqlMembers.total == 0
        assert zqlMembers.inventories.isEmpty()
        snapshots.each {
            def size = new GetVolumeSnapshotSizeAction(uuid: it.uuid, sessionId: session).call()
            assert size.error?.code == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString()
        }
    }

    private void testSharedVmDoesNotGrantDetailAccess() {
        shareResource {
            resourceUuids = [vm.uuid]
            accountUuids = [unrelated.uuid]
        }
        assertInvisible(unrelatedSession.uuid)
        assertDenied(group, unrelatedSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        def update = new UpdateVolumeSnapshotGroupAction(uuid: group.uuid,
                name: "shared-account-must-not-update", sessionId: unrelatedSession.uuid).call()
        assert update.error?.code == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString()
        revokeResourceSharing {
            resourceUuids = [vm.uuid]
            accountUuids = [unrelated.uuid]
        }
    }

    private void testDirectSnapshotSharing() {
        String snapshot = snapshots[0].uuid
        shareResource {
            resourceUuids = [snapshot]
            accountUuids = [unrelated.uuid]
        }
        assert zql("query volumeSnapshot where uuid='${snapshot}' return with (total)", unrelatedSession.uuid).total == 1
        assert new GetVolumeSnapshotSizeAction(uuid: snapshot, sessionId: unrelatedSession.uuid).call().error == null
        revokeResourceSharing {
            resourceUuids = [snapshot]
            accountUuids = [unrelated.uuid]
        }
        assertInvisible(unrelatedSession.uuid)
    }

    private void testDetachedDataVolumeRemainsReadable() {
        detachDataVolumeFromVm {
            uuid = dataVolume.uuid
            sessionId = adminSession()
        }
        assertReadable(ownerSession.uuid)
    }

    private void testStandaloneSnapshotKeepsOriginalAccess() {
        VolumeSnapshotInventory standalone = createVolumeSnapshot {
            name = "standalone-admin-snapshot"
            volumeUuid = vm.rootVolumeUuid
            sessionId = adminSession()
        } as VolumeSnapshotInventory
        assert zql("query volumeSnapshot where uuid='${standalone.uuid}' return with (total)", ownerSession.uuid).total == 0
        def size = new GetVolumeSnapshotSizeAction(uuid: standalone.uuid, sessionId: ownerSession.uuid).call()
        assert size.error?.code == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString()
    }

    private void testVmTransferMovesInheritedAccess() {
        changeResourceOwner {
            resourceUuid = vm.uuid
            accountUuid = unrelated.uuid
        }
        assert Account.getAccountUuidOfResource(group.uuid) == unrelated.uuid
        // Changing a volume owner already transfers its snapshots. The detached data disk stays with the old owner.
        assert Account.getAccountUuidOfResource(snapshots[1].uuid) == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        assertInvisible(ownerSession.uuid)
        assertReadable(unrelatedSession.uuid)
        // Reattach the data disk after the original detached-disk query assertions.
        changeResourceOwner {
            resourceUuid = dataVolume.uuid
            accountUuid = unrelated.uuid
        }
        attachDataVolumeToVm {
            vmInstanceUuid = vm.uuid
            volumeUuid = dataVolume.uuid
        }
        VolumeSnapshotGroupInventory transferredGroup = createGroup(adminSession())
        assert Account.getAccountUuidOfResource(transferredGroup.uuid) == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        assertDenied(transferredGroup, ownerSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        assertCreatedVm(transferredGroup, unrelatedSession, unrelated.uuid)
    }

    private void testVmCreationAccess() {
        Map<String, String> originalOwners = resourceOwners(group)
        assert originalOwners[group.uuid] == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        group.volumeSnapshotRefs.each {
            assert originalOwners[it.volumeSnapshotUuid] == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID
        }
        assertCreatedVm(group, ownerSession, vmOwnerAccount.uuid)
        assert resourceOwners(group) == originalOwners
        testInheritedAccessStaysWithinConfirmedGroupScope(group)
        assertCreatedVm(group, null, AccountConstant.INITIAL_SYSTEM_ADMIN_UUID)
        assert resourceOwners(group) == originalOwners
        assertDenied(group, unrelatedSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        shareResource {
            resourceUuids = [group.uuid]
            accountUuids = [unrelated.uuid]
        }
        assertCreatedVm(group, unrelatedSession, unrelated.uuid)
        assert resourceOwners(group) == originalOwners
        revokeResourceSharing {
            resourceUuids = [group.uuid]
            accountUuids = [unrelated.uuid]
        }
        assertDenied(group, unrelatedSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        assertDenied(group, noVmRoleSession, SysErrors.OPERATION_DENIED)
        VolumeSnapshotGroupInventory ownGroup = createGroup(ownerSession.uuid)
        assert Account.getAccountUuidOfResource(ownGroup.uuid) == vmOwnerAccount.uuid
        assertCreatedVm(ownGroup, ownerSession, vmOwnerAccount.uuid)
        assert resourceOwners(group) == originalOwners
    }

    private VolumeSnapshotGroupInventory createGroup(String session) {
        List<VolumeSnapshotInventory> snapshots = [vm.rootVolumeUuid, dataVolume.uuid].collect { String volume ->
            createVolumeSnapshot {
                name = "group-create-vm-snapshot-${groups.size()}-${volume}"
                volumeUuid = volume
                sessionId = session
            } as VolumeSnapshotInventory
        }
        List<org.zstack.header.storage.snapshot.VolumeSnapshotInventory> fixture = snapshots.collect {
            JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(it), org.zstack.header.storage.snapshot.VolumeSnapshotInventory.class)
        }
        env.message(CreateVolumesSnapshotMsg.class) { CreateVolumesSnapshotMsg msg, CloudBus bus ->
            assert msg.volumeSnapshotJobs.collect { it.volumeUuid }.toSet() == [vm.rootVolumeUuid, dataVolume.uuid].toSet()
            CreateVolumesSnapshotReply reply = new CreateVolumesSnapshotReply()
            reply.inventories = fixture
            bus.reply(msg, reply)
        }
        VolumeSnapshotGroupInventory group = createVolumeSnapshotGroup {
            name = "group-create-vm-${groups.size()}"
            rootVolumeUuid = vm.rootVolumeUuid
            sessionId = session
        } as VolumeSnapshotGroupInventory
        groups.add(group)
        assert group.snapshotCount == 2
        assert group.volumeSnapshotRefs.size() == 2
        assert group.volumeSnapshotRefs.every { !it.snapshotDeleted }
        return group
    }

    private CreateVmInstanceFromVolumeSnapshotGroupAction.Result createFromGroup(VolumeSnapshotGroupInventory group, SessionInventory session) {
        return new CreateVmInstanceFromVolumeSnapshotGroupAction(
                name: "group-create-vm-clone-${creationIndex++}",
                volumeSnapshotGroupUuid: group.uuid,
                cpuNum: 1, memorySize: SizeUnit.GIGABYTE.toByte(1), type: "UserVm",
                l3NetworkUuids: [l3Uuid], defaultL3NetworkUuid: l3Uuid,
                strategy: "InstantStart", sessionId: session == null ? adminSession() : session.uuid).call()
    }

    private void testInheritedAccessStaysWithinConfirmedGroupScope(VolumeSnapshotGroupInventory group) {
        // Controlled metadata fixtures exercise the permission boundary without running other clone modes.
        VolumeSnapshotGroupRefInventory dataRef = group.volumeSnapshotRefs.find { it.volumeType == "Data" }
        try {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, true).update()
            assertDenied(group, ownerSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        } finally {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.snapshotDeleted, false).update()
        }
        try {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.volumeType, "Memory").update()
            assertDenied(group, ownerSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        } finally {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.volumeType, "Data").update()
        }
        try {
            SQL.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, group.uuid)
                    .set(VolumeSnapshotGroupVO_.snapshotCount, 3).update()
            assertDenied(group, ownerSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        } finally {
            SQL.New(VolumeSnapshotGroupVO.class).eq(VolumeSnapshotGroupVO_.uuid, group.uuid)
                    .set(VolumeSnapshotGroupVO_.snapshotCount, 2).update()
        }
        String missingSnapshotUuid = UUID.randomUUID().toString().replace("-", "")
        assert (queryVolumeSnapshot {
            conditions = ["uuid=${missingSnapshotUuid}".toString()]
        } as List<VolumeSnapshotInventory>).isEmpty()
        try {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, missingSnapshotUuid).update()
            assertDenied(group, ownerSession, SysErrors.RESOURCE_NOT_ACCESSIBLE)
        } finally {
            SQL.New(VolumeSnapshotGroupRefVO.class)
                    .eq(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, missingSnapshotUuid)
                    .set(VolumeSnapshotGroupRefVO_.volumeSnapshotUuid, dataRef.volumeSnapshotUuid).update()
        }
    }

    private void assertCreatedVm(VolumeSnapshotGroupInventory group, SessionInventory session, String accountUuid) {
        def result = createFromGroup(group, session)
        assert result.error == null : result.error
        VmInstanceInventory created = result.value.inventory
        assert created != null && created.uuid != vm.uuid
        createdVmUuids.add(created.uuid)
        VmInstanceInventory queried = (queryVmInstance {
            conditions = ["uuid=${created.uuid}".toString()]
        } as List<VmInstanceInventory>)[0]
        assert queried.state == "Running"
        assert queried.allVolumes.size() == 2
        assert queried.allVolumes.collect { it.type }.toSet() == ["Root", "Data"].toSet()
        assert queried.allVolumes.every { it.uuid != vm.rootVolumeUuid && it.uuid != dataVolume.uuid }
        assert Account.getAccountUuidOfResource(queried.uuid) == accountUuid
        queried.allVolumes.each { assert Account.getAccountUuidOfResource(it.uuid) == accountUuid }
    }

    private void assertDenied(VolumeSnapshotGroupInventory group, SessionInventory session, SysErrors expected) {
        Set<String> before = (queryVmInstance {} as List<VmInstanceInventory>).collect { it.uuid }.toSet()
        def result = createFromGroup(group, session)
        assert result.error?.code == expected.toString()
        assert (queryVmInstance {} as List<VmInstanceInventory>).collect { it.uuid }.toSet() == before
    }

    private Map<String, String> resourceOwners(VolumeSnapshotGroupInventory group) {
        List<String> resources = [vm.uuid, vm.rootVolumeUuid, dataVolume.uuid, group.uuid]
        resources.addAll(group.volumeSnapshotRefs.collect { it.volumeSnapshotUuid })
        return resources.collectEntries { [(it): Account.getAccountUuidOfResource(it)] }
    }

    private ZQLQueryReturn zql(String statement, String session) {
        def result = new ZQLQueryAction(zql: statement, sessionId: session).call()
        assert result.error == null
        return result.value.results[0]
    }

    private AccountInventory accountWithRoles(String accountName, String... roles) {
        AccountInventory account = createAccount {
            name = accountName
            password = "password"
        } as AccountInventory
        attachPredefineRoles(account.uuid, roles.length == 0 ? ["vm", "volume", "snapshot"] as String[] : roles)
        return account
    }

    private SessionInventory login(AccountInventory account) {
        return logInByAccount {
            accountName = account.name
            password = "password"
        } as SessionInventory
    }

    @Override
    void clean() {
        createdVmUuids.reverseEach { String vmUuid ->
            destroyVmInstance { uuid = vmUuid; sessionId = adminSession() }
        }
        // Groups need their source VM; remove them before EnvSpec destroys it.
        groups.reverseEach { VolumeSnapshotGroupInventory snapshotGroup ->
            deleteVolumeSnapshotGroup { uuid = snapshotGroup.uuid; sessionId = adminSession() }
        }
        env.delete()
    }
}
