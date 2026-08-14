package org.zstack.compute.host;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.compute.vm.NextVolumeDeviceIdGetter;
import org.zstack.core.componentloader.PluginRegistry;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.SimpleQuery;
import org.zstack.header.host.HostPortVO;
import org.zstack.header.host.HostPortVO_;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.BitSet;
import java.util.List;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class HostPortGetter {
    private static final CLogger logger = Utils.getLogger(NextVolumeDeviceIdGetter.class);

    @Autowired
    private PluginRegistry pluginRgty;
    @Autowired
    private DatabaseFacade dbf;

    public HostPortVO getNextHostPort(String hostUuid, String usage) {
        SimpleQuery<HostPortVO> q = dbf.createQuery(HostPortVO.class);
        q.select(HostPortVO_.port);
        q.add(HostPortVO_.hostUuid, SimpleQuery.Op.EQ, hostUuid);
        q.orderBy(HostPortVO_.port, SimpleQuery.Od.ASC);
        List<Integer> values = q.listValue();

        BitSet full = new BitSet(values.size() + 1);
        full.set(0, HostGlobalConfig.HOST_PORT_ALLOCATION_START_PORT.value(Integer.class));
        values.forEach(full::set);
        Integer port = full.nextClearBit(0);

        HostPortVO hostPortVO = new HostPortVO();
        hostPortVO.setPort(port);
        hostPortVO.setHostUuid(hostUuid);
        hostPortVO.setPortUsage(usage);
        dbf.persist(hostPortVO);

        return hostPortVO;
    }
}
