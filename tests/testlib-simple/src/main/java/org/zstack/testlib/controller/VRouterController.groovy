package org.zstack.testlib.controller

import org.springframework.http.HttpEntity
import org.zstack.appliancevm.ApplianceVmStatus
import org.zstack.appliancevm.ApplianceVmVO
import org.zstack.appliancevm.ApplianceVmVO_
import org.zstack.core.db.Q
import org.zstack.network.service.virtualrouter.VirtualRouterCommands
import org.zstack.network.service.virtualrouter.VirtualRouterConstant
import org.zstack.testlib.ApiHelper
import org.zstack.testlib.EnvSpec
import org.zstack.testlib.Test
import org.zstack.testlib.util.Retry
import org.zstack.utils.gson.JSONObjectUtil

class VRouterController {
    class API extends ApiHelper implements Retry {
    }

    private EnvSpec env
    private API api = new API()
    private String sessionUuid

    private Set<String> disconnectedUuids = Collections.synchronizedSet(new HashSet<String>())

    VRouterController(EnvSpec env) {
        this.env = env
        sessionUuid = env.session.uuid

        env.afterSimulator(VirtualRouterConstant.VR_INIT) { rsp, HttpEntity<String> e ->
            def cmd =  JSONObjectUtil.toObject(e.body, VirtualRouterCommands.InitCommand.class)
            if (disconnectedUuids.contains(cmd.uuid)) {
                throw new Exception("VRouterController puts it down")
            }

            return rsp
        }
    }

    void disconnect(String vrUuid) {
        disconnectedUuids.add(vrUuid)

        Test.expect(AssertionError.class) {
            api.reconnectVirtualRouter {
                vmInstanceUuid = vrUuid
                sessionId = sessionUuid
            }
        }

        api.retryInSecs {
            ApplianceVmVO vr = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, vrUuid).find()
            assert vr.status == ApplianceVmStatus.Disconnected
        }
    }

    void connect(String vrUuid) {
        disconnectedUuids.remove(vrUuid)

        api.reconnectVirtualRouter {
            vmInstanceUuid = vrUuid
            sessionId = sessionUuid
        }

        api.retryInSecs {
            ApplianceVmVO vr = Q.New(ApplianceVmVO.class).eq(ApplianceVmVO_.uuid, vrUuid).find()
            assert vr.status == ApplianceVmStatus.Connected
        }
    }
}
