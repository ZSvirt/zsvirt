package org.zstack.mttyDevice;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.compute.host.PostHostConnectExtensionPoint;
import org.zstack.core.asyncbatch.While;
import org.zstack.core.db.Q;
import org.zstack.header.core.Completion;
import org.zstack.header.core.WhileDoneCompletion;
import org.zstack.header.core.workflow.Flow;
import org.zstack.header.core.workflow.FlowTrigger;
import org.zstack.header.core.workflow.NoRollbackFlow;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.errorcode.ErrorCodeList;
import org.zstack.header.host.HostInventory;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO;
import org.zstack.pciDevice.virtual.vfio_mdev.MdevDeviceVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * try to re-splite mtty devices in host because se vifo-mdev devices
 * cannot persist when the host is rebooted.
 *
 * see: ZSTAC-51096
 */
public class MttyResplitIntoMdevAfterHostConnectFlowFactory implements PostHostConnectExtensionPoint {
    private static final CLogger logger = Utils.getLogger(MttyResplitIntoMdevAfterHostConnectFlowFactory.class);

    @Autowired
    private MttyDeviceManager mttyDeviceManager;

    @Override
    public Flow createPostHostConnectFlow(HostInventory host) {
        return new NoRollbackFlow() {

            String __name__ = "resplit-mtty-devices-into-mdev-devices";

            @Override
            public void run(FlowTrigger trigger, Map data) {
                List<MttyDeviceVO> mttys = Q.New(MttyDeviceVO.class)
                        .eq(MttyDeviceVO_.hostUuid, host.getUuid())
                        .eq(MttyDeviceVO_.virtStatus, MttyDeviceVirtStatus.VFIO_MDEV_VIRTUALIZED)
                        .list();
                if (mttys.isEmpty()) {
                    trigger.next();
                    return;
                }

                logger.debug(String.format("try to re-splite mtty devices[uuid:%s] into mdev devices",
                        mttys.stream().map(MttyDeviceVO::getUuid).collect(Collectors.toList())));
                new While<>(mttys).each((mtty, comp) -> {
                    List<String> mdevUuids = Q.New(MdevDeviceVO.class)
                            .eq(MdevDeviceVO_.mttyUuid, mtty.getUuid())
                            .select(MdevDeviceVO_.uuid)
                            .listValues();
                    MttyDeviceBackend bkd = mttyDeviceManager.getMttyDeviceBackendByHostUuid(host.getUuid());
                    bkd.generateSeMdevDevices(host.getUuid(), mtty.toInventory(), mdevUuids, true, new Completion(comp) {
                        @Override
                        public void success() {
                            logger.debug(String.format("tried to re-splited mtty device[uuid:%s] into mdev devices", mtty.getUuid()));
                            comp.done();
                        }

                        @Override
                        public void fail(ErrorCode errorCode) {
                            logger.error(String.format("failed to re-splited mtty device[uuid:%s] into mdev devices", mtty.getUuid()));
                            comp.done();
                        }
                    });
                }).run(new WhileDoneCompletion(trigger) {
                    @Override
                    public void done(ErrorCodeList errorCodeList) {
                        trigger.next();
                    }
                });
            }
        };
    }
}
