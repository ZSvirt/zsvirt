package org.zstack.compute.allocator;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.core.db.SimpleQuery.Op;
import org.zstack.header.allocator.AbstractHostAllocatorFlow;
import org.zstack.header.allocator.DiskOfferingTagAllocatorExtensionPoint;
import org.zstack.header.allocator.HostCandidate;
import org.zstack.header.allocator.InstanceOfferingTagAllocatorExtensionPoint;
import org.zstack.header.configuration.DiskOfferingInventory;
import org.zstack.header.configuration.DiskOfferingVO;
import org.zstack.header.tag.SystemTagInventory;
import org.zstack.header.tag.SystemTagVO;
import org.zstack.header.tag.SystemTagVO_;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.List;

/**
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class TagAllocatorFlow extends AbstractHostAllocatorFlow {
    private static final CLogger logger = Utils.getLogger(TagAllocatorFlow.class);

    @Autowired
    private DatabaseFacade dbf;
    @Autowired
    private PluginRegistry pluginRgty;

    private List<InstanceOfferingTagAllocatorExtensionPoint> instanceOfferingExtensions;
    private List<DiskOfferingTagAllocatorExtensionPoint> diskOfferingExtensions;

    public TagAllocatorFlow() {
        instanceOfferingExtensions = pluginRgty.getExtensionList(InstanceOfferingTagAllocatorExtensionPoint.class);
        diskOfferingExtensions = pluginRgty.getExtensionList(DiskOfferingTagAllocatorExtensionPoint.class);
    }

    @Override
    public void allocate() {
        List<HostCandidate> tmp = new ArrayList<>(candidates);

        if (!instanceOfferingExtensions.isEmpty()) {
            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.add(SystemTagVO_.resourceType, Op.EQ, VmInstanceVO.class.getSimpleName());
            q.add(SystemTagVO_.resourceUuid, Op.EQ, spec.getVmInstance().getUuid());
            List<SystemTagVO> tvos = q.list();
            if (!tvos.isEmpty()) {
                List tinvs = SystemTagInventory.valueOf(tvos);
                for (InstanceOfferingTagAllocatorExtensionPoint extp : instanceOfferingExtensions) {
                    extp.allocateHost(tinvs, tmp, spec);
                    tmp.removeIf(candidate -> candidate.reject != null);

                    if (tmp.isEmpty()) {
                        return;
                    }
                }
            }
        }

        if (!diskOfferingExtensions.isEmpty() && spec.getDiskOfferings() != null && !spec.getDiskOfferings().isEmpty()) {
            List<String> diskOfferingUuids = CollectionUtils.transform(spec.getDiskOfferings(), DiskOfferingInventory::getUuid);

            SimpleQuery<SystemTagVO> q  = dbf.createQuery(SystemTagVO.class);
            q.add(SystemTagVO_.resourceType, Op.EQ, DiskOfferingVO.class.getSimpleName());
            q.add(SystemTagVO_.resourceUuid, Op.IN, diskOfferingUuids);
            List<SystemTagVO> tvos = q.list();
            if (!tvos.isEmpty()) {
                List tinvs = SystemTagInventory.valueOf(tvos);
                for (DiskOfferingTagAllocatorExtensionPoint extp : diskOfferingExtensions) {
                    extp.allocateHost(tinvs, tmp, spec);
                    tmp.removeIf(candidate -> candidate.reject != null);

                    if (tmp.isEmpty()) {
                        return;
                    }
                }
            }
        }

        next();
    }
}
