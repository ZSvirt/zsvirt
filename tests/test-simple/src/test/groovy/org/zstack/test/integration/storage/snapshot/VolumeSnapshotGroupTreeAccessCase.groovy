package org.zstack.test.integration.storage.snapshot

import org.zstack.core.cloudbus.CloudBus
import org.zstack.core.Platform
import org.zstack.header.errorcode.SysErrors
import org.zstack.header.identity.AccountConstant
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotMsg
import org.zstack.header.storage.snapshot.CreateVolumesSnapshotReply
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotDirection
import org.zstack.header.storage.snapshot.DeleteVolumeSnapshotScope
import org.zstack.sdk.AccountInventory
import org.zstack.sdk.ApiException
import org.zstack.sdk.GetVolumeSnapshotGroupTreeAction
import org.zstack.sdk.SessionInventory
import org.zstack.sdk.UpdateVolumeSnapshotGroupAction
import org.zstack.sdk.VmInstanceInventory
import org.zstack.sdk.VolumeSnapshotGroupInventory
import org.zstack.sdk.VolumeSnapshotGroupTreeInventory
import org.zstack.sdk.VolumeSnapshotInventory
import org.zstack.test.integration.storage.Env
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.SubCase
import org.zstack.utils.gson.JSONObjectUtil

class VolumeSnapshotGroupTreeAccessCase extends SubCase {
    EnvSpec env
    VmInstanceInventory vm
    AccountInventory vmOwner
    AccountInventory unrelatedAccount
    SessionInventory ownerSession
    SessionInventory unrelatedSession
    VolumeSnapshotGroupInventory snapshotGroup

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
            prepareVmOwnerAndSnapshotGroup()
            testAdminCanQuerySnapshotGroupTree()
            testVmOwnerCanQueryAdminCreatedSnapshotGroupTree()
            testUnrelatedAccountCannotQuerySnapshotGroupTree()
            testSharedVmCanBeQueriedButNotManaged()
            testUnrelatedAccountCannotManageSnapshotGroup()
            testVmOwnerCanManageAdminCreatedSnapshotGroup()
            testMissingVmUuidIsRejected()
            testInvalidVmUuidIsRejected()
        }
    }

    private void prepareVmOwnerAndSnapshotGroup() {
        vm = env.inventoryByName("vm") as VmInstanceInventory
        vmOwner = createAccountWithSnapshotRoles("snapshot-group-owner")
        unrelatedAccount = createAccountWithSnapshotRoles("snapshot-group-unrelated")
        ownerSession = login(vmOwner)
        unrelatedSession = login(unrelatedAccount)

        changeResourceOwner {
            resourceUuid = vm.uuid
            accountUuid = vmOwner.uuid
        }

        String actualVmOwnerUuid = accountUuidOf(vm.uuid)
        assert actualVmOwnerUuid == vmOwner.uuid :
                "VM owner was not changed: expected=${vmOwner.uuid}, actual=${actualVmOwnerUuid}, vmUuid=${vm.uuid}"

        VolumeSnapshotInventory snapshot = createVolumeSnapshot {
            name = "admin-created-root-volume-snapshot"
            volumeUuid = vm.rootVolumeUuid
            sessionId = adminSession()
        } as VolumeSnapshotInventory
        def snapshotInventory = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(snapshot),
                org.zstack.header.storage.snapshot.VolumeSnapshotInventory.class)
        int createSnapshotsCallCount = 0
        env.message(CreateVolumesSnapshotMsg.class) { CreateVolumesSnapshotMsg msg, CloudBus bus ->
            createSnapshotsCallCount++
            assert msg.volumeSnapshotJobs.size() == 1 :
                    "core fixture expects one root-volume snapshot job: actual=${msg.volumeSnapshotJobs.size()}, " +
                            "vmUuid=${vm.uuid}"
            assert msg.volumeSnapshotJobs[0].volumeUuid == vm.rootVolumeUuid :
                    "snapshot-group job must target the VM root volume: expected=${vm.rootVolumeUuid}, " +
                            "actual=${msg.volumeSnapshotJobs[0].volumeUuid}"

            CreateVolumesSnapshotReply reply = new CreateVolumesSnapshotReply()
            reply.inventories = [snapshotInventory]
            bus.reply(msg, reply)
        }

        snapshotGroup = createVolumeSnapshotGroup {
            name = "admin-created-snapshot-group"
            rootVolumeUuid = vm.rootVolumeUuid
            sessionId = adminSession()
        } as VolumeSnapshotGroupInventory
        assert createSnapshotsCallCount == 1 :
                "snapshot-group creation must issue one batch request: expected=1, actual=${createSnapshotsCallCount}"

        String actualGroupOwnerUuid = accountUuidOf(snapshotGroup.uuid)
        assert actualGroupOwnerUuid == AccountConstant.INITIAL_SYSTEM_ADMIN_UUID :
                "snapshot group must remain admin-owned: expected=${AccountConstant.INITIAL_SYSTEM_ADMIN_UUID}, " +
                        "actual=${actualGroupOwnerUuid}, groupUuid=${snapshotGroup.uuid}"
    }

    private void testAdminCanQuerySnapshotGroupTree() {
        List<VolumeSnapshotGroupTreeInventory> trees = getVolumeSnapshotGroupTree {
            vmInstanceUuid = vm.uuid
            sessionId = adminSession()
        } as List<VolumeSnapshotGroupTreeInventory>

        assertTreeContainsSnapshotGroup(trees, "admin")
    }

    private void testVmOwnerCanQueryAdminCreatedSnapshotGroupTree() {
        List<VolumeSnapshotGroupTreeInventory> expectedTrees = getVolumeSnapshotGroupTree {
            vmInstanceUuid = vm.uuid
            sessionId = adminSession()
        } as List<VolumeSnapshotGroupTreeInventory>
        GetVolumeSnapshotGroupTreeAction.Result result = new GetVolumeSnapshotGroupTreeAction(
                vmInstanceUuid: vm.uuid, sessionId: ownerSession.uuid).call()

        assert result.error == null :
                "VM owner must query admin-created snapshot groups: vmUuid=${vm.uuid}, ownerUuid=${vmOwner.uuid}, " +
                        "error=${JSONObjectUtil.toJsonString(result.error)}"
        assert result.value != null :
                "VM owner query returned no value: vmUuid=${vm.uuid}, ownerUuid=${vmOwner.uuid}"
        List expected = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(expectedTrees), List.class)
        List actual = JSONObjectUtil.toObject(JSONObjectUtil.toJsonString(result.value.inventories), List.class)
        assert actual == expected :
                "VM owner must see the same snapshot-group tree as admin: vmUuid=${vm.uuid}, " +
                        "expected=${JSONObjectUtil.toJsonString(expected)}, actual=${JSONObjectUtil.toJsonString(actual)}"
    }

    private void testUnrelatedAccountCannotQuerySnapshotGroupTree() {
        GetVolumeSnapshotGroupTreeAction.Result result = new GetVolumeSnapshotGroupTreeAction(
                vmInstanceUuid: vm.uuid, sessionId: unrelatedSession.uuid).call()

        assert result.error != null :
                "unrelated account must not query snapshot groups: vmUuid=${vm.uuid}, accountUuid=${unrelatedAccount.uuid}"
        String actualErrorCode = extractFromSDKError(result.error).code
        assert actualErrorCode == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString() :
                "unexpected access error: expected=${SysErrors.RESOURCE_NOT_ACCESSIBLE}, actual=${actualErrorCode}, " +
                        "error=${JSONObjectUtil.toJsonString(result.error)}"
        assert result.value == null :
                "denied snapshot-group query must not return data: vmUuid=${vm.uuid}, accountUuid=${unrelatedAccount.uuid}"
    }

    private void testVmOwnerCanManageAdminCreatedSnapshotGroup() {
        VolumeSnapshotGroupInventory updated = updateVolumeSnapshotGroup {
            uuid = snapshotGroup.uuid
            name = "updated-by-vm-owner"
            sessionId = ownerSession.uuid
        } as VolumeSnapshotGroupInventory
        assert updated.name == "updated-by-vm-owner"

        stopVmInstance {
            uuid = vm.uuid
            sessionId = adminSession()
        }
        def revertResult = revertVmFromSnapshotGroup {
            uuid = snapshotGroup.uuid
            sessionId = ownerSession.uuid
        }
        assert revertResult.results.success.every { it }

        deleteVolumeSnapshotGroup {
            uuid = snapshotGroup.uuid
            direction = DeleteVolumeSnapshotDirection.Commit.toString()
            scope = DeleteVolumeSnapshotScope.Single.toString()
            sessionId = ownerSession.uuid
        }
        assert (queryVolumeSnapshotGroup {
            conditions = ["uuid=${snapshotGroup.uuid}"]
            sessionId = adminSession()
        } as List).isEmpty()
    }

    private void testSharedVmCanBeQueriedButNotManaged() {
        shareResource {
            resourceUuids = [vm.uuid]
            accountUuids = [unrelatedAccount.uuid]
        }
        GetVolumeSnapshotGroupTreeAction.Result queryResult = new GetVolumeSnapshotGroupTreeAction(
                vmInstanceUuid: vm.uuid, sessionId: unrelatedSession.uuid).call()
        assert queryResult.error == null
        assert queryResult.value?.inventories != null

        UpdateVolumeSnapshotGroupAction.Result updateResult = new UpdateVolumeSnapshotGroupAction(
                uuid: snapshotGroup.uuid,
                name: "shared-account-must-not-update",
                sessionId: unrelatedSession.uuid).call()
        assert updateResult.error?.code == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString()

        revokeResourceSharing {
            resourceUuids = [vm.uuid]
            accountUuids = [unrelatedAccount.uuid]
        }
        testUnrelatedAccountCannotQuerySnapshotGroupTree()
    }

    private void testUnrelatedAccountCannotManageSnapshotGroup() {
        UpdateVolumeSnapshotGroupAction.Result result = new UpdateVolumeSnapshotGroupAction(
                uuid: snapshotGroup.uuid,
                name: "must-not-update",
                sessionId: unrelatedSession.uuid).call()
        assert result.error != null
        assert result.error.code == SysErrors.RESOURCE_NOT_ACCESSIBLE.toString()
        assert result.value == null
    }

    private void testMissingVmUuidIsRejected() {
        expect(ApiException.class) {
            getVolumeSnapshotGroupTree {}
        }
    }

    private void testInvalidVmUuidIsRejected() {
        [Platform.uuid, "not-a-vm-uuid"].each { String invalidVmUuid ->
            GetVolumeSnapshotGroupTreeAction.Result result = new GetVolumeSnapshotGroupTreeAction(
                    vmInstanceUuid: invalidVmUuid, sessionId: adminSession()).call()
            assert result.error != null
            assert extractFromSDKError(result.error).code == SysErrors.RESOURCE_NOT_FOUND.toString()
            assert result.value == null
        }
    }

    private AccountInventory createAccountWithSnapshotRoles(String name) {
        AccountInventory account = createAccount {
            delegate.name = name
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

    private String accountUuidOf(String resourceUuid) {
        return org.zstack.identity.Account.getAccountUuidOfResource(resourceUuid)
    }

    private void assertTreeContainsSnapshotGroup(List<VolumeSnapshotGroupTreeInventory> trees, String caller) {
        assert trees != null :
                "snapshot-group tree query returned null: caller=${caller}, vmUuid=${vm.uuid}"

        Map<String, VolumeSnapshotGroupTreeInventory> nodes = flattenTrees(trees)
        VolumeSnapshotGroupTreeInventory node = nodes[snapshotGroup.uuid]
        assert node != null :
                "snapshot group is missing from query result: caller=${caller}, expected=${snapshotGroup.uuid}, " +
                        "actual=${nodes.keySet()}"
        assert node.refs != null :
                "snapshot-group refs are missing: caller=${caller}, groupUuid=${snapshotGroup.uuid}"
        assert node.refs.size() == snapshotGroup.snapshotCount :
                "snapshot-group member count differs: caller=${caller}, expected=${snapshotGroup.snapshotCount}, " +
                        "actual=${node.refs.size()}, groupUuid=${snapshotGroup.uuid}"
        node.refs.each { ref ->
            assert ref.snapshot != null :
                    "live snapshot-group member has no snapshot payload: caller=${caller}, groupUuid=${snapshotGroup.uuid}, " +
                            "snapshotUuid=${ref.volumeSnapshotUuid}"
            assert ref.snapshot.uuid == ref.volumeSnapshotUuid :
                    "snapshot payload UUID differs from the group ref: caller=${caller}, expected=${ref.volumeSnapshotUuid}, " +
                            "actual=${ref.snapshot.uuid}, groupUuid=${snapshotGroup.uuid}"
        }
    }

    private Map<String, VolumeSnapshotGroupTreeInventory> flattenTrees(
            List<VolumeSnapshotGroupTreeInventory> trees) {
        Map<String, VolumeSnapshotGroupTreeInventory> nodes = new LinkedHashMap<>()
        Deque<VolumeSnapshotGroupTreeInventory> pending = new ArrayDeque<>(trees)
        while (!pending.isEmpty()) {
            VolumeSnapshotGroupTreeInventory node = pending.removeFirst()
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
}
