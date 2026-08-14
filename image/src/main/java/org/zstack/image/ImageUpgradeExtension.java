package org.zstack.image;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusListCallBack;
import org.zstack.core.cloudbus.EventCallback;
import org.zstack.core.cloudbus.EventFacade;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.UpdateQuery;
import org.zstack.header.Component;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.image.*;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents;
import org.zstack.header.storage.backup.BackupStorageCanonicalEvents.BackupStorageStatusChangedData;
import org.zstack.header.storage.backup.BackupStorageStatus;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.StringDSL;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.zstack.utils.CollectionUtils.transform;

/**
 * Created by xing5 on 2016/5/6.
 */
public class ImageUpgradeExtension implements Component {
    private static final CLogger logger = Utils.getLogger(ImageUpgradeExtension.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private EventFacade evtf;
    @Autowired
    private CloudBus bus;

    @Override
    public boolean start() {
        if (ImageGlobalProperty.SYNC_IMAGE_ACTUAL_SIZE_ON_START) {
            syncImageActualSize();
        }

        if (ImageGlobalProperty.FIX_IMAGE_CACHE_UUID) {
            fixImageCacheUuid();
        }

        return true;
    }

    @Transactional
    private void fixImageCacheUuid() {
        String sql = "select c, pri.type from ImageCacheVO c, PrimaryStorageVO pri where c.primaryStorageUuid = pri.uuid" +
                " and c.imageUuid is null";

        TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
        List<Tuple> ts = q.getResultList();
        for (Tuple t : ts) {
            ImageCacheVO c = t.get(0, ImageCacheVO.class);
            String psType = t.get(1, String.class);

            String imgUuid;
            if ("Ceph".equals(psType)) {
                imgUuid = c.getInstallUrl().split("@")[1];
            } else if ("NFS".equals(psType) || "SharedMountPoint".equals(psType)) {
                imgUuid = new File(c.getInstallUrl()).getName().split("\\.")[0];
            } else if ("LocalStorage".equals(psType)) {
                String[] pair = c.getInstallUrl().split(";");
                imgUuid = new File(pair[0]).getName().split("\\.")[0];
            } else {
                throw new CloudRuntimeException(String.format("unknown primary storage type[%s] for the ImageCacheVO[id:%s]",
                        psType, c.getId()));
            }

            if (!StringDSL.isZStackUuid(imgUuid)) {
                throw new CloudRuntimeException(String.format("the image UUID[%s] parsed from the URL[%s] of the ImageCacheVO[id:%s] " +
                        "on the primary storage[type:%s] looks no correct", imgUuid, c.getInstallUrl(), c.getId(), psType));
            }

            c.setImageUuid(imgUuid);
            dbf.getEntityManager().merge(c);
        }
    }

    private void syncImageActualSize() {
        evtf.on(BackupStorageCanonicalEvents.BACKUP_STORAGE_STATUS_CHANGED, new EventCallback() {
            @Transactional(readOnly = true)
            private List<ImageInventory> getImagesForSync(String bsUuid) {
                String sql = "select img from ImageVO img, ImageBackupStorageRefVO ref where" +
                        " img.uuid = ref.imageUuid and ref.backupStorageUuid = :bsUuid";
                TypedQuery<ImageVO> q = dbf.getEntityManager().createQuery(sql, ImageVO.class);
                q.setParameter("bsUuid", bsUuid);
                List<ImageVO> vos = q.getResultList();
                return ImageInventory.valueOf(vos);
            }

            @Override
            public void run(Map tokens, Object data) {
                if (!evtf.isFromThisManagementNode(tokens)) {
                    return;
                }

                final BackupStorageStatusChangedData d = (BackupStorageStatusChangedData) data;

                if (!BackupStorageStatus.Connected.toString().equals(d.getNewStatus())) {
                    return;
                }

                final List<ImageInventory> imgs = getImagesForSync(d.getBackupStorageUuid());
                if (imgs.isEmpty()) {
                    return;
                }

                List<SyncImageSizeMsg> msgs = transform(imgs, arg -> {
                    SyncImageSizeMsg msg = new SyncImageSizeMsg();
                    msg.setBackupStorageUuid(d.getBackupStorageUuid());
                    msg.setImageUuid(arg.getUuid());
                    bus.makeTargetServiceIdByResourceUuid(msg, ImageConstant.SERVICE_ID, arg.getUuid());
                    return msg;
                });

                bus.send(msgs, new CloudBusListCallBack(null) {
                    @Override
                    public void run(List<MessageReply> replies) {
                        Map<String, Long> res = new HashMap<>();
                        for (MessageReply r : replies) {
                            ImageInventory image = imgs.get(replies.indexOf(r));

                            if (!r.isSuccess()) {
                                logger.warn(String.format("failed to sync image [%s] size: %s",
                                        image.getUuid(), r.getError().getDetails()));
                                continue;
                            }

                            SyncImageSizeReply reply = r.castReply();
                            if (!res.containsKey(image.getUuid())) {
                                res.put(image.getUuid(), reply.getActualSize());
                            }
                        }

                        for (Map.Entry<String, Long> entry: res.entrySet()) {
                            UpdateQuery.New(ImageCacheVO.class)
                                    .set(ImageCacheVO_.size, entry.getValue())
                                    .condAnd(ImageCacheVO_.imageUuid, SimpleQuery.Op.EQ, entry.getKey())
                                    .update();
                        }

                        // Ask primary storage service to recalculate the capacities
                        final List<RecalculatePrimaryStorageCapacityMsg> rmsgs = dbf.listAll(PrimaryStorageVO.class).stream()
                                .map((psvo) -> {
                                    RecalculatePrimaryStorageCapacityMsg rmsg = new RecalculatePrimaryStorageCapacityMsg();
                                    rmsg.setPrimaryStorageUuid(psvo.getUuid());
                                    bus.makeLocalServiceId(rmsg, PrimaryStorageConstant.SERVICE_ID);
                                    return rmsg;
                                }).collect(Collectors.toList());
                        bus.send(rmsgs);
                    }
                });
            }
        });
    }

    @Override
    public boolean stop() {
        return true;
    }
}
