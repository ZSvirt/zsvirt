package org.zstack.test.integration.storage.primary.local

import org.zstack.core.db.Q
import org.zstack.core.db.SQL
import org.zstack.header.host.HostStatus
import org.zstack.header.host.HostVO
import org.zstack.header.host.HostVO_
import org.zstack.header.storage.primary.PrimaryStorageVO
import org.zstack.sdk.AddLocalPrimaryStorageAction
import org.zstack.sdk.ClusterInventory
import org.zstack.sdk.CreateSystemTagAction
import org.zstack.sdk.HostInventory
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.sdk.UpdateSystemTagAction
import org.zstack.storage.primary.local.LocalStorageHostRefVO
import org.zstack.test.integration.storage.StorageTest
import org.zstack.testlib.*

/**
 * Created by camile on 2017/4/
 */
class AddLocalStorageCase extends SubCase {
    EnvSpec env

    @Override
    void clean() {
        env.delete()
    }

    @Override
    void setup() {
        useSpring(StorageTest.springSpec)
    }

    @Override
    void environment() {
        env = env {
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
                    }
                }

                localPrimaryStorage {
                    name = "local"
                    url = "/local_ps"
                }
            }
        }
    }

    @Override
    void test() {
        env.create {
            addErrorPathLSailure()
            testAttachLocalStorage()
            updatePrimaryStorageCidr()
        }
    }

    void updatePrimaryStorageCidr() {
        def primaryStorageInventory = env.inventoryByName("local") as PrimaryStorageInventory
        String zoneUuid1 = (env.specByName("zone") as ZoneSpec).inventory.uuid

        String tag1 = "primaryStorage::gateway::cidr::2.2.3.1/24"
        String tag2 = "primaryStorage::gateway::cidr::2.2.4.1/24"

        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction1 = new AddLocalPrimaryStorageAction(
                url: "/zstack_p4",
                name: "p4",
                type: "LocalStorage",
                systemTags: Arrays.asList(tag1),
                zoneUuid: zoneUuid1,
                sessionId: adminSession()
        )
        AddLocalPrimaryStorageAction.Result result1 = addLocalPrimaryStorageAction1.call()
        assert result1.error == null

        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction2 = new AddLocalPrimaryStorageAction(
                url: "/zstack_p5",
                name: "p5",
                type: "LocalStorage",
                systemTags: Arrays.asList("primaryStorage::gateway::cidr::sssss"),
                zoneUuid: zoneUuid1,
                sessionId: adminSession()
        )
        AddLocalPrimaryStorageAction.Result result2 = addLocalPrimaryStorageAction2.call()
        assert result2.error != null

        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction3 = new AddLocalPrimaryStorageAction(
                url: "/zstack_p6",
                name: "p6",
                type: "LocalStorage",
                systemTags: Arrays.asList(tag1, tag2),
                zoneUuid: zoneUuid1,
                sessionId: adminSession()
        )
        AddLocalPrimaryStorageAction.Result result3 = addLocalPrimaryStorageAction3.call()
        assert result3.error != null

        CreateSystemTagAction createSystemTagAction1 = new CreateSystemTagAction(
                resourceType: PrimaryStorageVO.getSimpleName(),
                resourceUuid: primaryStorageInventory.uuid,
                tag: "primaryStorage::gateway::cidr::1.1.1.1/24",
                sessionId: adminSession()
        )
        CreateSystemTagAction.Result res1 = createSystemTagAction1.call()
        assert res1.error == null

        CreateSystemTagAction createSystemTagAction2 = new CreateSystemTagAction(
                resourceType: PrimaryStorageVO.getSimpleName(),
                resourceUuid: primaryStorageInventory.uuid,
                tag: "primaryStorage::gateway::cidr::dasdasdas",
                sessionId: adminSession()
        )
        CreateSystemTagAction.Result res2 = createSystemTagAction2.call()
        assert res2.error != null

        UpdateSystemTagAction updateSystemTagAction1 = new UpdateSystemTagAction(
                tag: "primaryStorage::gateway::cidr::dadsasd",
                uuid: res1.value.inventory.uuid,
                sessionId: adminSession()
        )
        UpdateSystemTagAction.Result res3 = updateSystemTagAction1.call()
        assert res3.error != null

        UpdateSystemTagAction updateSystemTagAction2 = new UpdateSystemTagAction(
                tag: "primaryStorage::gateway::cidr::1.1.2.2/22",
                uuid: res1.value.inventory.uuid,
                sessionId: adminSession()
        )
        UpdateSystemTagAction.Result res4 = updateSystemTagAction2.call()
        assert res4.error == null
    }

    void addErrorPathLSailure() {
        String zoneUuid = (env.specByName("zone") as ZoneSpec).inventory.uuid
        AddLocalPrimaryStorageAction addLocalPrimaryStorageAction = new AddLocalPrimaryStorageAction()
        addLocalPrimaryStorageAction.url = "/dev/test"
        addLocalPrimaryStorageAction.name = "test2"
        addLocalPrimaryStorageAction.zoneUuid = zoneUuid
        addLocalPrimaryStorageAction.sessionId = adminSession()
        AddLocalPrimaryStorageAction.Result res = addLocalPrimaryStorageAction.call()
        assert res.error != null
        addLocalPrimaryStorageAction.url = "/proc/test"
        res = addLocalPrimaryStorageAction.call()
        assert res.error != null
        addLocalPrimaryStorageAction.url = "/sys/test"
        res = addLocalPrimaryStorageAction.call()
        assert res.error != null
    }

    void testAttachLocalStorage() {
        def cluster = env.inventoryByName("cluster") as ClusterInventory
        def host = env.inventoryByName("kvm") as HostInventory
        def ls = env.inventoryByName("local") as PrimaryStorageInventory
        SQL.New(HostVO.class).set(HostVO_.status, HostStatus.Connecting).eq(HostVO_.uuid, host.uuid).update()
        attachPrimaryStorageToCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ls.uuid
        }
        retryInSecs(3) {
            assert Q.New(LocalStorageHostRefVO.class).count() == 1
        }

        detachPrimaryStorageFromCluster {
            clusterUuid = cluster.uuid
            primaryStorageUuid = ls.uuid
        }
    }
}
