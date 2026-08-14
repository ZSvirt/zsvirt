package org.zstack.testlib.controller

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.storage.backup.BackupStorageStatus
import org.zstack.header.storage.backup.BackupStorageVO
import org.zstack.header.storage.backup.BackupStorageVO_
import org.zstack.sdk.BackupStorageInventory
import org.zstack.storage.backup.BackupStorageBase
import org.zstack.testlib.ApiHelper
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.testlib.util.Retry
import org.zstack.testlib.util.TProxy

class BackupStorageController {
    class API extends ApiHelper implements Retry {
    }

    private EnvSpec env
    private API api = new API()
    private String sessionUuid

    private Set<String> disconnectedUuids = Collections.synchronizedSet(new HashSet<String>())

    BackupStorageController(EnvSpec env) {
        this.env = env
        sessionUuid = env.session.uuid

        env.mockFactory(BackupStorageBase.class) { BackupStorageBase bs ->
            def proxy = new TProxy(bs)
            proxy.mockMethod("connectHook") { invokeSuper, boolean newAdd, Completion completion ->
                if (disconnectedUuids.contains(bs.self.uuid)) {
                    completion.fail(Platform.operr("BackupStorageController puts it down"))
                } else {
                    return invokeSuper()
                }
            }

            return proxy as BackupStorageBase
        }
    }

    void disconnect(String name) {
        BackupStorageInventory inv = env.inventoryByName(name)
        assert inv != null : "cannot find backup storage[${name}]"

        disconnectedUuids.add(inv.uuid)
        Test.expect(AssertionError.class) {
            api.reconnectBackupStorage {
                uuid = inv.uuid
                sessionId = sessionUuid
            }
        }

        api.retryInSecs {
            BackupStorageVO bs = Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, inv.uuid).find()
            assert bs.status == BackupStorageStatus.Disconnected
        }
    }

    void connect(String name) {
        BackupStorageInventory inv = env.inventoryByName(name)
        assert inv != null : "cannot find backup storage[${name}]"

        disconnectedUuids.remove(inv.uuid)

        api.reconnectBackupStorage {
            uuid = inv.uuid
            sessionId = sessionUuid
        }

        api.retryInSecs {
            BackupStorageVO bs = Q.New(BackupStorageVO.class).eq(BackupStorageVO_.uuid, inv.uuid).find()
            assert bs.status == BackupStorageStatus.Connected
        }
    }
}
