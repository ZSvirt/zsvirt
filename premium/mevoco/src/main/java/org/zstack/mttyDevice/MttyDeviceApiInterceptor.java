package org.zstack.mttyDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.Platform;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.apimediator.ApiMessageInterceptor;
import org.zstack.header.apimediator.InterceptorForService;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;
import org.zstack.header.message.APIMessage;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceStatus;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;

import java.util.List;

/**
 * @author yu.sun
 * @date 2022/11/22 14:29
 **/
@InterceptorForService("mttyDevice")
public class MttyDeviceApiInterceptor implements ApiMessageInterceptor {
    @Autowired
    private DatabaseFacade dbf;
    
    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIGenerateSeMdevDevicesMsg) {
            validate((APIGenerateSeMdevDevicesMsg) msg);
        } else if (msg instanceof APIUngenerateSeMdevDevicesMsg) {
            validate((APIUngenerateSeMdevDevicesMsg) msg);
        }
        return msg;
    }
    
    private void validate(APIUngenerateSeMdevDevicesMsg msg) {
        MttyDeviceVO mtty = dbf.findByUuid(msg.getMttyDeviceUuid(), MttyDeviceVO.class);
        if (mtty.getVirtStatus() != MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("mtty device[uuid:%s] is not virtualized into mdevs", msg.getMttyDeviceUuid())
            );
        }
        
        List<String> mdevUuids = Q.New(MdevDeviceVO.class)
                .eq(MdevDeviceVO_.mttyUuid, msg.getMttyDeviceUuid())
                .select(MdevDeviceVO_.uuid)
                .listValues();
        if (mdevUuids != null && !mdevUuids.isEmpty()) {
            msg.setMdevUuids(mdevUuids);
        }
        
        boolean exists = Q.New(MdevDeviceVO.class)
                .in(MdevDeviceVO_.uuid, mdevUuids)
                .eq(MdevDeviceVO_.status, MdevDeviceStatus.Attached)
                .isExists();
        if (exists) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "mdev devices generated from mtty device[uuid:%s] still attached to vm", msg.getMttyDeviceUuid()));
        }
        
        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, mtty.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that mtty device[uuid:%s] in is not Connected",
                    mtty.getHostUuid(), mtty.getUuid()
            ));
        }
    }
    
    private void validate(APIGenerateSeMdevDevicesMsg msg) {
        MttyDeviceVO mtty = dbf.findByUuid(msg.getMttyDeviceUuid(), MttyDeviceVO.class);
        if (mtty.getVirtStatus() != MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZABLE && mtty.getVirtStatus() != MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED) {
            throw new ApiMessageInterceptionException(
                    Platform.argerr("mtty device[uuid:%s] cannot be virtualized into mdevs", msg.getMttyDeviceUuid())
            );
        }
        
        boolean connected = Q.New(HostVO.class)
                .eq(HostVO_.uuid, mtty.getHostUuid())
                .eq(HostVO_.status, HostStatus.Connected)
                .isExists();
        if (!connected) {
            throw new ApiMessageInterceptionException(Platform.argerr(
                    "the host[uuid:%s] that mtty device[uuid:%s] in is not Connected",
                    mtty.getHostUuid(), mtty.getUuid()
            ));
        }
    }
}