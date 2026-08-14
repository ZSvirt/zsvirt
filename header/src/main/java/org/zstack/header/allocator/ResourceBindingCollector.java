package org.zstack.header.allocator;

import org.zstack.header.host.HostVO;

import java.util.List;

public interface ResourceBindingCollector {

    String getType();

    List<HostVO> collect(List<String> resourceUuids);
}
