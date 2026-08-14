package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;
import java.util.List;

/**
 * Created by lining on 2017/4/11.
 */
public interface MarshalResultFunction {
    void marshal(List<HostVO> hosts);
}
