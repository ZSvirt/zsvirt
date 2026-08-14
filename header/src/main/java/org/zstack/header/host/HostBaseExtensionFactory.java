package org.zstack.header.host;

import java.util.List;

/**
 * Created by mingjian.deng on 16/12/1.
 */
public interface HostBaseExtensionFactory {
    Host getHost(HostVO vo);

    List<Class> getMessageClasses();
}
