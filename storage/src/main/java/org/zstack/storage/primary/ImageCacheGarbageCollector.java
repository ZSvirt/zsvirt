package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.cloudbus.CloudBusCallBack;
import org.zstack.core.config.GlobalConfig;
import org.zstack.core.config.GlobalConfigUpdateExtensionPoint;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.thread.PeriodicTask;
import org.zstack.core.thread.ThreadFacade;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeChangeListener;
import org.zstack.header.managementnode.ManagementNodeInventory;
import org.zstack.header.message.MessageReply;
import org.zstack.header.storage.primary.*;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class ImageCacheGarbageCollector implements Component, ManagementNodeChangeListener, PeriodicTask {
    private static CLogger logger = Utils.getLogger(ImageCacheGarbageCollector.class);
    
    @Autowired
    private ThreadFacade thdf;
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    
    private int garbageCollectionInterval;
    private Future<Void> garbageCollectionThread;
    
    @Override
    public boolean start() {
        garbageCollectionInterval = PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.value(Integer.class);
        PrimaryStorageGlobalConfig.IMAGE_CACHE_GARBAGE_COLLECTOR_INTERVAL.installUpdateExtension(new GlobalConfigUpdateExtensionPoint() {
            @Override
            public void updateGlobalConfig(GlobalConfig oldConfig, GlobalConfig newConfig) {
                garbageCollectionInterval = newConfig.value(Integer.class);
                startGarbageCollectionThread();
            }
        });
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    private synchronized void startGarbageCollectionThread() {
        if (garbageCollectionThread != null) {
            garbageCollectionThread.cancel(true);
        }

        garbageCollectionThread = thdf.submitPeriodicTask(this);
        logger.debug(String.format("Image cache garbage collector starts running by interval[%ss]", garbageCollectionInterval));
    }
    
    @Override
    public void nodeJoin(ManagementNodeInventory inv) {
        startGarbageCollectionThread();
    }

    @Override
    public void nodeLeft(ManagementNodeInventory inv) {
    }

    @Override
    public void iAmDead(ManagementNodeInventory inv) {
    }

    @Override
    public void iJoin(ManagementNodeInventory inv) {
    }

    @Transactional
    private List<ImageCacheVO> getImageCacheToDelete() {
        String sql = "select i from ImageCacheVO i where i.imageUuid = NULL and i.state = :state";
        TypedQuery<ImageCacheVO> q = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        q.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q.setParameter("state", ImageCacheState.ready);
        List<ImageCacheVO> ret = q.getResultList();
        
        // if ImageCacheVO in deleting state and it has been stayed for 1 day
        // that means zstack that issued garbage collection exited before removing this entry from database
        // we garbage this entry again here
        sql = "select i from ImageCacheVO i where i.imageUuid = NULL and i.state = :state and CURRENT_TIMESTAMP > DATE_ADD(i.lastOpDate, INTERVAL 1 DAY)";
        Query q1 = dbf.getEntityManager().createNativeQuery(sql, ImageCacheVO.class);
        q1.setLockMode(LockModeType.PESSIMISTIC_WRITE);
        q1.setParameter("state", ImageCacheState.deleting);
        ret.addAll(q1.getResultList());
        if (ret.isEmpty()) {
            return ret;
        }
        
        List<Long> ids = new ArrayList<Long>(ret.size());
        for (ImageCacheVO i : ret) {
            ids.add(i.getId());
        }
        sql = "update ImageCacheVO i set i.state = :state where i.id in (:ids)";
        TypedQuery<ImageCacheVO> q2 = dbf.getEntityManager().createQuery(sql, ImageCacheVO.class);
        q2.setParameter("state", ImageCacheState.deleting);
        q2.setParameter("ids", ids);
        q2.executeUpdate();
        return ret;
    }
    
    private void deleteImageCacheOnPrimaryStorage(final ImageCacheVO ic) {
        PrimaryStorageRemoveCachedImageMsg msg = new PrimaryStorageRemoveCachedImageMsg();
        msg.setInventory(ImageCacheInventory.valueOf(ic));
        bus.makeTargetServiceIdByResourceUuid(msg, PrimaryStorageConstant.SERVICE_ID, ic.getPrimaryStorageUuid());
        bus.send(msg, new CloudBusCallBack(null) {
            private void fail(String error) {
                ic.setState(ImageCacheState.ready);
                dbf.update(ic);
                logger.warn(String.format("failed to garbage collect image cache[id:%s, install:%s] on primary storage[uuid:%s], because %s. Change its state back to ready and try garbage collecting it next time", ic.getId(), ic.getInstallUrl(), ic.getPrimaryStorageUuid(), error));
            }
            
            private void success() {
                dbf.remove(ic);
                logger.debug(String.format("successfully garbage collected image cache[id:%s, install url:%s] on primary storage[uuid:%s]", ic.getId(), ic.getInstallUrl(), ic.getPrimaryStorageUuid()));
            }
            
            @Override
            public void run(MessageReply reply) {
                if (reply.isSuccess()) {
                    success();
                } else {
                    fail(reply.getError().toString());
                }
            }
        });
    }
    
    @Override
    public void run() {
        try {
            List<ImageCacheVO> ics = getImageCacheToDelete();
            for (ImageCacheVO i : ics) {
                deleteImageCacheOnPrimaryStorage(i);
            }
        } catch (Exception e) {
            logger.warn(e.getMessage(), e);
        }
    }

    @Override
    public TimeUnit getTimeUnit() {
        return TimeUnit.SECONDS;
    }

    @Override
    public long getInterval() {
        return garbageCollectionInterval;
    }

    @Override
    public String getName() {
        return this.getClass().getSimpleName();
    }
}
