package org.zstack.testlib.controller

import org.zstack.core.Platform
import org.zstack.core.db.Q
import org.zstack.header.core.Completion
import org.zstack.header.storage.primary.*
import org.zstack.sdk.PrimaryStorageInventory
import org.zstack.storage.primary.PrimaryStorageBase
import org.zstack.testlib.ApiHelper
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.testlib.util.Retry
import org.zstack.testlib.util.TProxy

class PrimaryStorageController {
    class API extends ApiHelper implements Retry {
    }

    private EnvSpec env
    private API api = new API()
    private String sessionUuid

    private Set<String> disconnectedUuids = Collections.synchronizedSet(new HashSet())

    PrimaryStorageController(EnvSpec env) {
        this.env = env
        sessionUuid = env.session.uuid

        env.mockFactory(PrimaryStorageBase.class) {
            PrimaryStorageBase ps = it
            TProxy proxy = new TProxy(ps)
            proxy.mockMethod("connectHook") { Closure invokeSuper, PrimaryStorageBase.ConnectParam param, Completion completion ->
                if (disconnectedUuids.contains(ps.self.uuid)) {
                    completion.fail(Platform.operr("PrimaryStorageController puts it down"))
                } else {
                    return invokeSuper()
                }
            }

            return proxy as PrimaryStorage
        }
    }

    void disconnect(String name) {
        PrimaryStorageInventory inv = env.inventoryByName(name)
        assert inv != null : "cannot find primary storage[${name}]"

        disconnectedUuids.add(inv.uuid)

        Test.expect(AssertionError.class) {
            api.reconnectPrimaryStorage {
                uuid = inv.uuid
                delegate.sessionId = sessionUuid
            }
        }

        api.retryInSecs {
            PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, inv.uuid).find()
            assert ps.status == PrimaryStorageStatus.Disconnected
        }
    }

    void connect(String name) {
        PrimaryStorageInventory inv = env.inventoryByName(name)
        assert inv != null : "cannot find primary storage[${name}"

        disconnectedUuids.remove(inv.uuid)
        api.reconnectPrimaryStorage {
            uuid = inv.uuid
            sessionId = sessionUuid
        }

        api.retryInSecs {
            PrimaryStorageVO ps = Q.New(PrimaryStorageVO.class).eq(PrimaryStorageVO_.uuid, inv.uuid).find()
            assert ps.status == PrimaryStorageStatus.Connected
        }
    }
}
