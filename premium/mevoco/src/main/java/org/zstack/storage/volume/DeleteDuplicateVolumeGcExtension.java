package org.zstack.storage.volume;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.transaction.annotation.Transactional;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.gc.GCStatus;
import org.zstack.core.gc.GarbageCollectorVO;
import org.zstack.core.gc.GarbageCollectorVO_;
import org.zstack.core.thread.AsyncThread;
import org.zstack.header.Component;
import org.zstack.header.managementnode.ManagementNodeReadyExtensionPoint;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Query;
import java.util.*;
import java.util.stream.Stream;

import static org.zstack.storage.volume.DeduplicateVolumeGcGlobalProperty.DEDUPLICATEVOLUMEGC;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class DeleteDuplicateVolumeGcExtension implements Component, ManagementNodeReadyExtensionPoint {
    protected static final CLogger logger = Utils.getLogger(DeleteDuplicateVolumeGcExtension.class);

    @Autowired
    protected DatabaseFacade dbf;

    class DeleteDuplicateVolumeGC {
        private Query query;
        private Integer first = 0;
        private Integer max = 1000;
        private long count = Q.New(GarbageCollectorVO.class).eq(GarbageCollectorVO_.status, GCStatus.Idle).count();
        private String sql = getSql();

        @AsyncThread
        @Transactional
        public void deleteDuplicate(){
            doDeleteDuplicate();
        }

        @Transactional
        public void doDeleteDuplicate() {
            query = dbf.getEntityManager().createQuery(sql);
            query.setMaxResults(max);
            query.setParameter("status", GCStatus.Idle);

            int times = (int) (count / max) + (count % max != 0 ? 1 : 0);
            HashSet<String> volumeUuids = new HashSet<>();
            for (int i = 0; i < times; i++) {
                query.setFirstResult(first);
                List<GarbageCollectorVO> vos = query.getResultList();
                dedup(vos, volumeUuids);
            }
        }

        private String getSql() {
            String sql = StringUtils.join(Stream.of("ceph", "shared-block", "nfs", "smp", "nfs", "delete-volume", "mini-storage",
                    "aliyun-ebs", "aliyun-nas").map(it -> " vo.name like 'gc-" + it + "%'").iterator(), " or");
            return String.format("select vo from GarbageCollectorVO vo where vo.status = :status and (%s)", sql);
        }

        private String getContextVolumeUuid(GarbageCollectorVO vo) {
            String context = vo.getContext();
            JsonObject jo = new JsonParser().parse(context).getAsJsonObject();
            return jo.get("volume").getAsJsonObject().get("uuid").getAsString();
        }

        private void dedup(List<GarbageCollectorVO> vos, HashSet<String> volumeUuids) {
            vos.forEach(vo -> {
                String volUuid = getContextVolumeUuid(vo);
                if (volumeUuids.contains(volUuid)) {
                    dbf.getEntityManager().remove(vo);
                } else {
                    volumeUuids.add(volUuid);
                    first += 1;
                }
            });
        }
    }

    @Override
    public boolean start() {
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }

    @Override
    public void managementNodeReady() {
        if (DEDUPLICATEVOLUMEGC) {
            new DeleteDuplicateVolumeGC().deleteDuplicate();
        }
    }
}