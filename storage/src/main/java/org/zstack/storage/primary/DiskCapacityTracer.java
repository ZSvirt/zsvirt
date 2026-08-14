package org.zstack.storage.primary;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.EntityEvent;
import org.zstack.core.db.EntityLifeCycleCallback;
import org.zstack.header.Component;
import org.zstack.header.core.workflow.*;
import org.zstack.header.storage.primary.ImageCacheVO;
import org.zstack.header.storage.primary.PrimaryStorageAllocationSpec;
import org.zstack.header.storage.primary.PrimaryStorageCapacityVO;
import org.zstack.header.storage.primary.PrimaryStorageConstant.AllocatorParams;
import org.zstack.header.storage.primary.PrimaryStorageVO;
import org.zstack.header.storage.snapshot.VolumeSnapshotEO;
import org.zstack.header.storage.snapshot.VolumeSnapshotVO;
import org.zstack.header.volume.VolumeAO;
import org.zstack.header.volume.VolumeEO;
import org.zstack.header.volume.VolumeVO;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by xing5 on 2016/5/10.
 */
public class DiskCapacityTracer implements Component {
    private static Logger logger = LogManager.getLogger("org.zstack.storage.primary.DiskCapacityTracer");
    private static Logger loggerd = LogManager.getLogger("org.zstack.storage.primary.DiskCapacityTracerDetails");
    private static CLogger clogger = Utils.getLogger(DiskCapacityTracer.class);

    @Autowired
    private DatabaseFacade dbf;

    public void trackAllocatorChain(FlowChain chain) {
        if (!PrimaryStorageGlobalProperty.CAPACITY_TRACKER_ON) {
            return;
        }

        chain.addProcessor(new FlowChainProcessor() {
            @Override
            public void beforeChainStart(FlowChainMutable chain) {
                List<Flow> flows = new ArrayList<Flow>();
                for (Flow f : chain.getFlows()) {
                    flows.add(f);
                    flows.add(new NoRollbackFlow() {
                        String __name__ = "disk-capacity-tracker";

                        @Override
                        public void run(FlowTrigger trigger, Map data) {
                            PrimaryStorageAllocationSpec spec = (PrimaryStorageAllocationSpec) data.get(AllocatorParams.SPEC);
                            List<PrimaryStorageVO> candidates = (List<PrimaryStorageVO>) data.get(AllocatorParams.CANDIDATES);
                            List<PrimaryStorageVO> all = dbf.listAll(PrimaryStorageVO.class);

                            StringBuilder sb = new StringBuilder("\n=== Primary Storage Allocation Tracker ====\n");
                            sb.append(String.format("REQUIRED SIZE: %s\n", spec.getSize()));
                            sb.append("CANDIDATES:\n");
                            for (PrimaryStorageVO p : candidates) {
                                sb.append(String.format("[name]:%s [uuid]:%s [status]:%s [state]:%s [available]:%s [total]:%s",
                                        p.getName(), p.getUuid(), p.getStatus(), p.getState(), p.getCapacity().getAvailableCapacity(), p.getCapacity().getTotalCapacity()));
                            }
                            sb.append("\n\n");
                            sb.append("ALL:\n");
                            for (PrimaryStorageVO p : all) {
                                sb.append(String.format("[name]:%s [uuid]:%s [status]:%s [state]:%s [available]:%s [total]:%s",
                                        p.getName(), p.getUuid(), p.getStatus(), p.getState(), p.getCapacity().getAvailableCapacity(), p.getCapacity().getTotalCapacity()));
                            }
                            sb.append("\n===========================================\n");
                            clogger.debug(sb.toString());
                            trigger.next();
                        }
                    });
                }

                chain.setFlows(flows);
            }
        });
    }

    private void printCallTrace() {
        StackTraceElement[] elements = Thread.currentThread().getStackTrace();
        List<String> lst = new ArrayList<String>();
        for (StackTraceElement el : elements) {
            if (el.getClassName().contains("org.zstack")) {
                lst.add(el.toString());
            }
        }
        loggerd.debug(StringUtils.join(lst, "\n"));
    }

    @Override
    public boolean start() {
        if (!PrimaryStorageGlobalProperty.CAPACITY_TRACKER_ON) {
            return true;
        }

        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                if (vol.getSize() != 0) {
                    String info = String.format("[Volume:Create][name=%s, uuid=%s, type=%s]: %s",
                            vol.getName(), vol.getUuid(), vol.getType(), vol.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeVO vol = (VolumeVO) o;
                VolumeAO pre = vol.getShadow();
                if (pre.getSize() != vol.getSize()) {
                    String info = String.format("[Volume:Update][name=%s, uuid=%s, type=%s]: %s --> %s",
                            vol.getName(), vol.getUuid(), vol.getType(), pre.getSize(), vol.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });

        dbf.installEntityLifeCycleCallback(VolumeEO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeEO vol = (VolumeEO) o;
                VolumeEO pre = (VolumeEO) vol.getShadow();

                if (pre.getDeleted() == null && vol.getDeleted() != null && vol.getSize() != 0) {
                    String info = String.format("[Volume:Delete][name=%s, uuid=%s, type=%s]: -%s", vol.getName(),
                            vol.getUuid(), vol.getType(), vol.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });

        dbf.installEntityLifeCycleCallback(ImageCacheVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                ImageCacheVO img = (ImageCacheVO) o;
                String info = String.format("[ImageCache:Create][uuid=%s]: %s", img.getImageUuid(), img.getSize());
                logger.debug(info);
                loggerd.debug(info);
                printCallTrace();
            }
        });
        dbf.installEntityLifeCycleCallback(ImageCacheVO.class, EntityEvent.POST_REMOVE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                ImageCacheVO img = (ImageCacheVO) o;
                String info = String.format("[ImageCache:Delete][uuid=%s]: -%s", img.getImageUuid(), img.getSize());
                logger.debug(info);
                loggerd.debug(info);
                printCallTrace();
            }
        });

        dbf.installEntityLifeCycleCallback(VolumeSnapshotVO.class, EntityEvent.POST_PERSIST, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeSnapshotVO s = (VolumeSnapshotVO) o;
                String info = String.format("[VolumeSnapshot:Create][name=%s, uuid=%s]: %s", s.getName(), s.getUuid(), s.getSize());
                logger.debug(info);
                loggerd.debug(info);
                printCallTrace();
            }
        });
        dbf.installEntityLifeCycleCallback(VolumeSnapshotEO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                VolumeSnapshotEO s = (VolumeSnapshotEO) o;
                VolumeSnapshotEO pre = (VolumeSnapshotEO) s.getShadow();
                if (pre.getDeleted() == null && s.getDeleted() != null) {
                    String info = String.format("[VolumeSnapshot:Delete][name=%s, uuid=%s]: -%s",
                            s.getName(),
                            s.getUuid(),
                            s.getSize());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });

        dbf.installEntityLifeCycleCallback(PrimaryStorageCapacityVO.class, EntityEvent.POST_UPDATE, new EntityLifeCycleCallback() {
            @Override
            public void entityLifeCycleEvent(EntityEvent evt, Object o) {
                PrimaryStorageCapacityVO c = (PrimaryStorageCapacityVO) o;
                PrimaryStorageCapacityVO pre = c.getShadow();
                if (c.getAvailableCapacity() != pre.getAvailableCapacity()) {
                    String info = String.format(
                            "[PrimaryStorageCapacity:Change][uuid=%s]: %s --> %s (%s)",
                            pre.getUuid(),
                            pre.getAvailableCapacity(),
                            c.getAvailableCapacity(),
                            c.getAvailableCapacity() - pre.getAvailableCapacity());
                    logger.debug(info);
                    loggerd.debug(info);
                    printCallTrace();
                }
            }
        });

        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
