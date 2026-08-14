package org.zstack.compute.vm;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.vm.BeforeGetNextVolumeDeviceIdExtensionPoint;
import org.zstack.header.volume.VolumeVO;
import org.zstack.header.volume.VolumeVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.BitSet;
import java.util.List;

/**
 * Create by weiwang at 2018/10/24
 */
@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class NextVolumeDeviceIdGetter {
    private static final CLogger logger = Utils.getLogger(NextVolumeDeviceIdGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;

    public Integer getNextVolumeDeviceId(String vmUuid) {
        SimpleQuery<VolumeVO> q = dbf.createQuery(VolumeVO.class);
        q.select(VolumeVO_.deviceId);
        q.add(VolumeVO_.vmInstanceUuid, SimpleQuery.Op.EQ, vmUuid);
        q.add(VolumeVO_.deviceId, SimpleQuery.Op.NOT_NULL);
        q.add(VolumeVO_.isShareable, SimpleQuery.Op.EQ, false);
        q.orderBy(VolumeVO_.deviceId, SimpleQuery.Od.ASC);
        List<Integer> devIds = q.listValue();

        for (BeforeGetNextVolumeDeviceIdExtensionPoint e : pluginRgty.getExtensionList(BeforeGetNextVolumeDeviceIdExtensionPoint.class)) {
            e.beforeGetNextVolumeDeviceId(vmUuid, devIds);
        }

        BitSet full = new BitSet(devIds.size() + 1);
        devIds.forEach(full::set);
        return full.nextClearBit(0);
    }
}
