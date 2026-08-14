package org.zstack.header.volume.block;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;

/**
 * @author shenjin
 * @date 2023/6/13 16:54
 */
@StaticMetamodel(XskyBlockVolumeVO.class)
public class XskyBlockVolumeVO_ extends BlockVolumeVO_ {
    public static volatile SingularAttribute<XskyBlockVolumeVO, Integer> accessPathId;
    public static volatile SingularAttribute<XskyBlockVolumeVO, String> accessPathIqn;
    public static volatile SingularAttribute<XskyBlockVolumeVO, String> xskyStatus;
    public static volatile SingularAttribute<XskyBlockVolumeVO, Integer> xskyBlockVolumeId;
    public static volatile SingularAttribute<XskyBlockVolumeVO, Long> burstTotalBw;
    public static volatile SingularAttribute<XskyBlockVolumeVO, Long> burstTotalIops;
    public static volatile SingularAttribute<XskyBlockVolumeVO, Long> maxTotalBw;
    public static volatile SingularAttribute<XskyBlockVolumeVO, Long> maxTotalIops;
}
