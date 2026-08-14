package org.zstack.storage.primary;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.cloudbus.CloudBus;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.header.storage.primary.*;
import org.zstack.header.volume.VolumeStatus;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.asList;

/**
 * Created by AlanJager on 2017/4/25.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStorageCapacityRecalculator {
    private static CLogger logger = Utils.getLogger(PrimaryStorageCapacityRecalculator.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private CloudBus bus;
    @Autowired
    private ErrorFacade errf;
    @Autowired
    private PrimaryStorageOverProvisioningManager ratioMgr;
    @Autowired
    private PluginRegistry pluginRgty;

    public List<String> psUuids;
    private Map<String, RecalculatePrimaryStorageCapacityExtensionPoint> recalculateCapacityExtensions = new HashMap<>();

    public PrimaryStorageCapacityRecalculator() {
        for (RecalculatePrimaryStorageCapacityExtensionPoint ext : pluginRgty.getExtensionList(RecalculatePrimaryStorageCapacityExtensionPoint.class)) {
            RecalculatePrimaryStorageCapacityExtensionPoint old = recalculateCapacityExtensions.get(ext.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint());
            if (old != null) {
                throw new CloudRuntimeException(
                        String.format("duplicate RecalculatePrimaryStorageCapacityExtensionPoint[%s, %s] for type[%s]",
                                ext.getClass().getName(), old.getClass().getName(),
                                old.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint()));
            }
            recalculateCapacityExtensions.put(ext.getPrimaryStorageTypeForRecalculateCapacityExtensionPoint(), ext);
        }
    }

    public void recalculate() {
        if (psUuids.isEmpty()) {
            return;
        }

        final Map<String, Long> psCap = new HashMap<>();
        new Runnable() {
            @Override
            @Transactional(readOnly = true)
            public void run() {
                // calculate all volume size
                {
                    String sql = "select sum(vol.size), vol.primaryStorageUuid" +
                            " from VolumeVO vol" +
                            " where vol.primaryStorageUuid in (:psUuids)" +
                            " and vol.status in (:volStatus)" +
                            " group by vol.primaryStorageUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", psUuids);
                    List<VolumeStatus> needCountVolumeStates = asList(VolumeStatus.Creating, VolumeStatus.Ready, VolumeStatus.Deleted);
                    q.setParameter("volStatus", needCountVolumeStates);
                    List<Tuple> ts = q.getResultList();

                    for (Tuple t : ts) {
                        if (t.get(0, Long.class) == null) {
                            // no volume
                            continue;
                        }

                        long cap = t.get(0, Long.class);
                        String psUuid = t.get(1, String.class);
                        psCap.put(psUuid, ratioMgr.calculateByRatio(psUuid, cap));
                    }
                }

                // calculate all image cache size
                {
                    String sql = "select sum(i.size), i.primaryStorageUuid" +
                            " from ImageCacheVO i" +
                            " where i.primaryStorageUuid in (:psUuids)" +
                            " group by i.primaryStorageUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", psUuids);
                    List<Tuple> ts = q.getResultList();
                    for (Tuple t : ts) {
                        if (t.get(0, Long.class) == null) {
                            // no image cache
                            continue;
                        }

                        // templates in image cache are physical size
                        // do not calculate over-provisioning
                        long cap = t.get(0, Long.class);
                        String psUuid = t.get(1, String.class);
                        Long ncap = psCap.get(psUuid);
                        ncap = ncap == null ? cap : ncap + cap;
                        psCap.put(psUuid, ncap);
                    }
                }

                // calculate all snapshot size
                {
                    String sql = "select sum(snapshot.size), snapshot.primaryStorageUuid" +
                            " from VolumeSnapshotVO snapshot" +
                            " where snapshot.primaryStorageUuid in (:psUuids)" +
                            " group by snapshot.primaryStorageUuid";
                    TypedQuery<Tuple> q = dbf.getEntityManager().createQuery(sql, Tuple.class);
                    q.setParameter("psUuids", psUuids);
                    List<Tuple> ts = q.getResultList();

                    for (Tuple t : ts) {
                        if (t.get(0, Long.class) == null) {
                            // no snapshot
                            continue;
                        }

                        long cap = t.get(0, Long.class);
                        String psUuid = t.get(1, String.class);
                        Long ncap = psCap.get(psUuid);
                        ncap = ncap == null ? cap : ncap + cap;
                        psCap.put(psUuid, ncap);
                    }
                }
            }
        }.run();


        if (psCap.isEmpty()) {
            // the primary storage is empty
            for (String psUuid : psUuids) {
                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        String sql = "select ps.type" +
                                " from PrimaryStorageVO ps" +
                                " where ps.uuid = :psUuid";
                        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                        q.setParameter("psUuid", psUuid);
                        String type = q.getSingleResult();

                        RecalculatePrimaryStorageCapacityExtensionPoint ext = recalculateCapacityExtensions.get(type);
                        RecalculatePrimaryStorageCapacityStruct struct = new RecalculatePrimaryStorageCapacityStruct();
                        struct.setPrimaryStorageUuid(psUuid);

                        if (ext != null) {
                            ext.beforeRecalculatePrimaryStorageCapacity(struct);
                        }

                        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(psUuid);
                        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                            @Override
                            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {

                                long before = cap.getAvailableCapacity();
                                long now = cap.getTotalCapacity()
                                        - (cap.getSystemUsedCapacity() == null ? 0 : cap.getSystemUsedCapacity());
                                cap.setAvailableCapacity(now);
                                logger.debug(String.format("re-calculated available capacity of the primary storage" +
                                                "[uuid:%s, before:%s, now:%s] with over-provisioning ratio[%s]",
                                        psUuid, before, now, ratioMgr.getRatio(psUuid)));
                                return cap;
                            }
                        });

                        if (ext != null) {
                            ext.afterRecalculatePrimaryStorageCapacity(struct);
                        }
                    }
                }.run();
            }
        } else {
            // there are volumes/images on the primary storage, re-calculate the available capacity
            for (final Map.Entry<String, Long> e : psCap.entrySet()) {
                final String psUuid = e.getKey();
                final long used = e.getValue();

                new Runnable() {
                    @Override
                    @Transactional
                    public void run() {
                        String sql = "select ps.type" +
                                " from PrimaryStorageVO ps" +
                                " where ps.uuid = :psUuid";
                        TypedQuery<String> q = dbf.getEntityManager().createQuery(sql, String.class);
                        q.setParameter("psUuid", psUuid);
                        String type = q.getSingleResult();

                        RecalculatePrimaryStorageCapacityExtensionPoint ext = recalculateCapacityExtensions.get(type);
                        RecalculatePrimaryStorageCapacityStruct struct = new RecalculatePrimaryStorageCapacityStruct();
                        struct.setPrimaryStorageUuid(psUuid);

                        if (ext != null) {
                            ext.beforeRecalculatePrimaryStorageCapacity(struct);
                        }

                        PrimaryStorageCapacityUpdater updater = new PrimaryStorageCapacityUpdater(psUuid);
                        updater.run(new PrimaryStorageCapacityUpdaterRunnable() {
                            @Override
                            public PrimaryStorageCapacityVO call(PrimaryStorageCapacityVO cap) {
                                long before = cap.getAvailableCapacity();
                                long now = cap.getTotalCapacity()
                                        - used
                                        - (cap.getSystemUsedCapacity() == null ? 0 : cap.getSystemUsedCapacity());
                                cap.setAvailableCapacity(now);
                                logger.debug(String.format("re-calculated available capacity of the primary storage" +
                                                "[uuid:%s, before:%s, now:%s] with over-provisioning ratio[%s]",
                                        psUuid, before, now, ratioMgr.getRatio(psUuid)));
                                return cap;
                            }
                        });

                        if (ext != null) {
                            ext.afterRecalculatePrimaryStorageCapacity(struct);
                        }
                    }
                }.run();
            }
        }
    }
}
