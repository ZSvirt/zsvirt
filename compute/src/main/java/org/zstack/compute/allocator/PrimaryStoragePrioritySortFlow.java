package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.allocator.AbstractHostSortorFlow;
import org.zstack.header.host.HostInventory;
import org.zstack.header.image.ImageBackupStorageRefVO;
import org.zstack.header.image.ImageBackupStorageRefVO_;
import org.zstack.header.image.ImageVO;
import org.zstack.header.storage.backup.PrimaryStoragePriorityGetter;
import org.zstack.utils.DebugUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by mingjian.deng on 2017/10/31.
 * the bellow flow adjust the priority of ps if it indicated in xml
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class PrimaryStoragePrioritySortFlow extends AbstractHostSortorFlow {
    private static CLogger logger = Utils.getLogger(PrimaryStoragePrioritySortFlow.class);
    @Autowired
    protected DatabaseFacade dbf;
    @Autowired
    protected PrimaryStoragePriorityGetter priorityGetter;

    private boolean skip = true;

    @Override
    public void sort() {
        DebugUtils.Assert(candidates != null && !candidates.isEmpty(), "HostInventory cannot be none");

        if (spec.getImage() == null) {
            prepareForNext(candidates);
            return;
        }

        boolean imageExistInAnyBs = Q.New(ImageBackupStorageRefVO.class)
                .eq(ImageBackupStorageRefVO_.imageUuid, spec.getImage().getUuid())
                .isExists();
        if (!imageExistInAnyBs) {
            prepareForNext(candidates);
            return;
        }

        if (spec.getRequiredPrimaryStorageUuids() != null && spec.getRequiredPrimaryStorageUuids().size() == 1) {
            prepareForNext(candidates);
            return;
        }

        PrimaryStoragePriorityGetter.PrimaryStoragePriority priority =
                priorityGetter.getPrimaryStoragePriority(spec.getImage().getUuid(), spec.getRequiredBackupStorageUuid());

        adjustCandidates(priority.psPriority, priority.defaultPriority);
    }

    private void adjustCandidates(List<PrimaryStoragePriorityGetter.PriorityMap> priMap, int defaultPriority) {
        if (priMap.size() == 0) {
            prepareForNext(candidates);
            return;
        }

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("before PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
        }

        String psPriorityCondition = String.join(" ", priMap.stream()
                .map(it -> String.format("when '%s' then %d", it.PS, it.priority))
                .collect(Collectors.toList()));
        logger.debug(String.format("ps priority condition : %s", psPriorityCondition));

        List<Tuple> ts = SQL.New("select min(case pr.type " +
                psPriorityCondition +
                " else :defaultPriority end) as priority, h.uuid" +
                " from PrimaryStorageClusterRefVO ref, HostVO h, ClusterVO c, PrimaryStorageVO pr" +
                " where h.uuid in (:huuids)" +
                " and h.clusterUuid=c.uuid" +
                " and c.uuid=ref.clusterUuid" +
                " and pr.uuid=ref.primaryStorageUuid " +
                " group by h.uuid", Tuple.class)
                .param("huuids", candidates.stream().map(HostInventory::getUuid).collect(Collectors.toList()))
                .param("defaultPriority", defaultPriority)
                .list();

        Map<String, Integer> hostPriority = new HashMap<>();
        Integer topPriority = ts.stream()
                .peek(it -> hostPriority.put(it.get(1, String.class), it.get(0, Integer.class)))
                .mapToInt(it -> it.get(0, Integer.class))
                .min().orElse(defaultPriority);

        // sort by priority
        List<HostInventory> sorted = candidates.stream()
                .sorted(Comparator.comparingInt(it -> hostPriority.get(it.getUuid())))
                .collect(Collectors.toList());
        candidates.clear();
        candidates.addAll(sorted);

        // choose hosts on top priority ps
        sorted.removeIf(it -> hostPriority.get(it.getUuid()) > topPriority);
        prepareForNext(sorted);

        if (logger.isTraceEnabled()) {
            logger.trace(String.format("subCandidates: %s",subCandidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
            logger.trace(String.format("after PrimaryStoragePrioritySortFlow adjustCandidates: %s", candidates.stream().map(HostInventory::getName).collect(Collectors.toList())));
        }
    }

    private void prepareForNext(List<HostInventory> hosts) {
        if (hosts != null && hosts.size() > 0) {
            subCandidates.clear();
            subCandidates.addAll(hosts);
            skip = false;
        }
    }

    @Override
    public boolean skipNext() {
        return skip;
    }
}
