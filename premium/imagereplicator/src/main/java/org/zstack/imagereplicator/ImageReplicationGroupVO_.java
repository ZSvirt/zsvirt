package org.zstack.imagereplicator;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageReplicationGroupVO.class)
public class ImageReplicationGroupVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<ImageReplicationGroupVO, String> name;
    public static volatile SingularAttribute<ImageReplicationGroupVO, String> description;
    public static volatile SingularAttribute<ImageReplicationGroupVO, ReplicationGroupState> state;
    public static volatile SingularAttribute<ImageReplicationGroupVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageReplicationGroupVO, Timestamp> lastOpDate;
}
