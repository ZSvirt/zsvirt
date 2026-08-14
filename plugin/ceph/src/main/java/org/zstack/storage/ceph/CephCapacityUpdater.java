package org.zstack.storage.ceph;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.GLock;
import javax.persistence.LockModeType;
import java.util.List;

/**
 * Created by frank on 7/28/2015.
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class CephCapacityUpdater {
    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    public void update(CephCapacity cephCapacity) {
        update(cephCapacity, true);
    }

    @Transactional
    public void update(CephCapacity cephCapacity, boolean updatedAnyway) {
        String fsid = cephCapacity.getFsid();
        long total = cephCapacity.getTotalCapacity();
        long avail = cephCapacity.getAvailableCapacity();

        CephCapacityVO vo = dbf.getEntityManager().find(CephCapacityVO.class, fsid, LockModeType.PESSIMISTIC_WRITE);
        boolean updated = false;

        if (vo == null) {
            GLock lock = new GLock(String.format("ceph-%s", fsid), 120);
            lock.lock();
            try {
                vo = dbf.getEntityManager().find(CephCapacityVO.class, fsid, LockModeType.PESSIMISTIC_WRITE);
                if (vo == null) {
                    vo = new CephCapacityVO();
                    vo.setFsid(fsid);
                    vo.setTotalCapacity(total);
                    vo.setAvailableCapacity(avail);
                    dbf.getEntityManager().persist(vo);
                    updated = true;
                } else {
                    if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                        vo.setTotalCapacity(total);
                        vo.setAvailableCapacity(avail);
                        dbf.getEntityManager().merge(vo);
                        updated = true;
                    }
                }
            } finally {
                lock.unlock();
            }
        } else  {
            if (vo.getAvailableCapacity() != avail || vo.getTotalCapacity() != total) {
                vo.setTotalCapacity(total);
                vo.setAvailableCapacity(avail);
                dbf.getEntityManager().merge(vo);
                updated = true;
            }
        }

        if (updatedAnyway || updated) {
            for (CephCapacityUpdateExtensionPoint ext : pluginRgty.getExtensionList(CephCapacityUpdateExtensionPoint.class)) {
                ext.update(cephCapacity);
            }
        }
    }
}
