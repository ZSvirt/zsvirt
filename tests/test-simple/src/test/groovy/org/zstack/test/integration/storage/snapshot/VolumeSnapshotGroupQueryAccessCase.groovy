package org.zstack.test.integration.storage.snapshot

import org.zstack.core.cloudbus.CloudBus
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.AccountConstant
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotMsg
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotReply
import org.zstack.identity.Account
import org.zstack.sdk.*
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

/** Real API authorization, Query/ZQL parsing and persistence; storage batch creation is a fixture. */
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

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = Env.localStorageOneVmWithOneDataVolumeEnv()
    }

    @Override
    void test() {
        env.create {
            prepareAdminGroupOnOwnedVm()
            assertReadable(ownerSession.uuid)
            assertReadable(adminSession())
            assertInvisible(unrelatedSession.uuid)
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
    }

    private ZQLQueryReturn zql(String statement, String session) {
        def result = new ZQLQueryAction(zql: statement, sessionId: session).call()
        assert result.error == null
        return result.value.results[0]
    }

    private AccountInventory accountWithRoles(String accountName) {
        AccountInventory account = createAccount {
            name = accountName
            password = "password"
        } as AccountInventory
        attachPredefineRoles(account.uuid, "vm", "volume", "snapshot")
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
        // Group deletion needs its VM; EnvSpec destroys the VM before API-created resources.
        if (group != null) {
            deleteVolumeSnapshotGroup {
                uuid = group.uuid
                sessionId = adminSession()
            }
        }
        env.delete()
    }
}
