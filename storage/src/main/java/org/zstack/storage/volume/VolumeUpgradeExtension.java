package org.zstack.storage.volume;

import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.Component;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.header.storage.primary.PrimaryStorageCanonicalEvent.PrimaryStorageStatusChangedData;
import org.zstack.header.volume.*;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;
import static org.zstack.utils.CollectionUtils.transformAndRemoveNull;

/**
 * Created by xing5 on 2016/5/25.
 */
public class VolumeUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(VolumeUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        if (VolumeGlobalProperty.SYNC_VOLUME_SIZE) {
            syncVolumeSize();
        }

        if (VolumeGlobalProperty.ROOT_VOLUME_FIND_MISSING_IMAGE_UUID) {
            rootVolumeFindMissingUuid();
        }

        return true;
    }

    private void rootVolumeFindMissingUuid() {
        List<VolumeVO> volumes = Q.New(VolumeVO.class)
                .eq(VolumeVO_.type, VolumeType.Root)
                .isNull(VolumeVO_.rootImageUuid)
                .list();
        if (volumes.isEmpty()) {
            return;
        }

        final Map<String, List<VolumeVO>> groupByPrimaryStorageUuid = new HashMap();
        for (VolumeVO vo : volumes) {
            List<VolumeVO> vos = groupByPrimaryStorageUuid.get(vo.getPrimaryStorageUuid());
            if (vos == null) {
                vos = new ArrayList();
                groupByPrimaryStorageUuid.put(vo.getPrimaryStorageUuid(), vos);
            }
            vos.add(vo);
        }

        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH, new EventCallback() {
            List<String> supportPsTypes = asList("NFS", "LocalStorage");

            @Override
            public void run(Map tokens, Object data) {
                PrimaryStorageStatusChangedData d = (PrimaryStorageStatusChangedData) data;

                if (!PrimaryStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    return;
                }

                final List<VolumeVO> vos = groupByPrimaryStorageUuid.get(d.getPrimaryStorageUuid());
                if (vos == null) {
                    return;
                }

                if (!supportPsTypes.contains(d.getInventory().getType())) {
                    return;
                }

                List<GetVolumeRootImageUuidFromPrimaryStorageMsg> msgs = transformAndRemoveNull(vos, arg -> {
                    GetVolumeRootImageUuidFromPrimaryStorageMsg msg = new GetVolumeRootImageUuidFromPrimaryStorageMsg();
                    msg.setVolume(VolumeInventory.valueOf(arg));
                    bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, arg.getPrimaryStorageUuid());
                    return msg;
                });

                for (final GetVolumeRootImageUuidFromPrimaryStorageMsg msg : msgs) {
                    bus.send(msg, new CloudBusCallBack(null) {
                        @Override
                        public void run(MessageReply reply) {
                            if (reply.isSuccess()) {
                                GetVolumeRootImageUuidFromPrimaryStorageReply r = reply.castReply();
                                VolumeVO vol = CollectionUtils.findOneOrNull(vos,
                                        arg -> arg.getUuid().equals(msg.getVolume().getUuid()));
                                vol.setRootImageUuid(r.getImageUuid());
                                dbf.update(vol);
                                vos.remove(vol);
                            } else  {
                                logger.warn(String.format("unable to get the rootImageUuid for the volume[uuid:%s], %s",
                                        msg.getVolume().getUuid(), reply.getError()));
                            }
                        }
                    });
                }
            }
        });
    }

    private void syncVolumeSize() {
        evtf.on(PrimaryStorageCanonicalEvent.PRIMARY_STORAGE_STATUS_CHANGED_PATH, new EventCallback() {
            @Override
            public void run(Map tokens, Object data) {
                if (!evtf.isFromThisManagementNode(tokens)) {
                    return;
                }

                PrimaryStorageStatusChangedData d = (PrimaryStorageStatusChangedData) data;

                if (!PrimaryStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    return;
                }

                List<VolumeVO> vols = Q.New(VolumeVO.class)
                        .eq(VolumeVO_.primaryStorageUuid, d.getPrimaryStorageUuid())
                        .eq(VolumeVO_.status, VolumeStatus.Ready)
                        .isNull(VolumeVO_.actualSize)
                        .list();

                if (vols.isEmpty()) {
                    return;
                }

                List<SyncVolumeSizeMsg> msgs = transformAndRemoveNull(vols, arg -> {
                    SyncVolumeSizeMsg msg = new SyncVolumeSizeMsg();
                    msg.setVolumeUuid(arg.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, VolumeConstant.SERVICE_ID, arg.getUuid());
                    return msg;
                });

                bus.send(msgs);
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
