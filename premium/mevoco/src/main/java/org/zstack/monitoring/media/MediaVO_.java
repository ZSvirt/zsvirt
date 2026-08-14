package org.zstack.monitoring.media;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by xing5 on 2017/6/11.
 */
@StaticMetamodel(MediaVO.class)
public class MediaVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<MediaVO, String> name;
    public static volatile SingularAttribute<MediaVO, String> description;
    public static volatile SingularAttribute<MediaVO, String> type;
    public static volatile SingularAttribute<MediaVO, MediaState> state;
    public static volatile SingularAttribute<MediaVO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<MediaVO, Timestamp> createDate;
}
