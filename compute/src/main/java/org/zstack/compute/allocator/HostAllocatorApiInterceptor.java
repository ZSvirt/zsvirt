package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityMsg;
import org.zstack.header.allocator.APIGetCpuMemoryCapacityReply;
import org.zstack.header.allocator.HostAllocatorConstant;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.apimediator.StopRoutingException;
import org.zstack.header.errorcode.SysErrors;
import org.zstack.header.image.APIGetCandidateBackupStorageForCreatingImageMsg;
import org.zstack.header.message.APIMessage;
import org.zstack.header.zone.ZoneVO;
import org.zstack.header.zone.ZoneVO_;

import static org.zstack.core.Platform.argerr;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: frank
 * Time: 4:16 PM
 * To change this template use File | Settings | File Templates.
 */
@InterceptorForService("host.allocator")
public class HostAllocatorApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private DatabaseFacade dbf;

    private void setServiceId(APIMessage msg) {
        bus.makeLocalServiceId(msg, HostAllocatorConstant.SERVICE_ID);
    }

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGetCpuMemoryCapacityMsg) {
            validate((APIGetCpuMemoryCapacityMsg) msg);
        } else if (msg instanceof APIGetCandidateBackupStorageForCreatingImageMsg) {
            validate((APIGetCandidateBackupStorageForCreatingImageMsg) msg);
        }

        setServiceId(msg);
        return msg;
    }

    private void validate(APIGetCandidateBackupStorageForCreatingImageMsg msg) {
        if (msg.getVolumeSnapshotUuid() == null && msg.getVolumeUuid() == null) {
            throw new ApiMessageInterceptionException(argerr(
                    "either volumeUuid or volumeSnapshotUuid must be set"
            ));
        }
    }

    private void validate(APIGetCpuMemoryCapacityMsg msg) {
        boolean pass = false;
        if ((msg.getZoneUuids() != null && !msg.getZoneUuids().isEmpty())) {
            pass = true;
        }
        if ((msg.getClusterUuids() != null && !msg.getClusterUuids().isEmpty())) {
            pass = true;
        }
        if ((msg.getHostUuids() != null && !msg.getHostUuids().isEmpty())) {
            pass = true;
        }

        if (!pass && !msg.isAll()) {
            throw new ApiMessageInterceptionException(argerr("zoneUuids, clusterUuids, hostUuids must at least have one be none-empty list, or all is set to true"));
        }

        if (msg.isAll() && (msg.getZoneUuids() == null || msg.getZoneUuids().isEmpty())) {
            SimpleQuery<ZoneVO> q = dbf.createQuery(ZoneVO.class);
            q.select(ZoneVO_.uuid);
            List<String> zuuids = q.listValue();
            msg.setZoneUuids(zuuids);
            msg.setHypervisorType(null);

            if (msg.getZoneUuids().isEmpty()) {
                APIGetCpuMemoryCapacityReply reply = new APIGetCpuMemoryCapacityReply();
                bus.reply(msg, reply);
                throw new StopRoutingException();
            }
        }
    }
}
