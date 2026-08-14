package org.zstack.compute.allocator;

import org.zstack.core.db.Q;
import org.zstack.header.allocator.ResourceBindingCollector;
import org.zstack.header.host.HostState;
import org.zstack.header.host.HostStatus;
import org.zstack.header.host.HostVO;
import org.zstack.header.host.HostVO_;

import java.util.List;

/**
 * @ Author : yh.w
 * @ Date   : Created in 11:20 2019/11/27
 */
public class ResourceBindingHostCollector implements ResourceBindingCollector {

    @Override
    public String getType() {
        return "Host";
    }

    @Override
    public List<HostVO> collect(List<String> uuids) {
        return Q.New(HostVO.class)
                .eq(HostVO_.status, HostStatus.Connected)
                .eq(HostVO_.state, HostState.Enabled)
                .in(HostVO_.uuid, uuids)
                .list();
    }
}
