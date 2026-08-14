package org.zstack.compute.host;

import org.zstack.header.host.HostVO;

import java.util.List;

/**
 * Created by yufan.wang on 2017/9/7.
 */
public interface VolumeMigrationTargetHostFilter {
    List<HostVO> filter(List<HostVO> candidates);
}
