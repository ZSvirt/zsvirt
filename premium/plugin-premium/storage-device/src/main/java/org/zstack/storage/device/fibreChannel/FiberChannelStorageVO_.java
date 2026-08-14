package org.zstack.storage.device.fibreChannel;

import org.zstack.header.vo.ResourceVO_;
import org.zstack.storage.device.iscsi.IscsiServerVO;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/10/18
 */
@StaticMetamodel(FiberChannelStorageVO.class)
public class FiberChannelStorageVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<FiberChannelStorageVO, String> state;
    public static volatile SingularAttribute<FiberChannelStorageVO, String> name;
    public static volatile SingularAttribute<FiberChannelStorageVO, String> wwnn;
    public static volatile SingularAttribute<FiberChannelStorageVO, Timestamp> createDate;
    public static volatile SingularAttribute<FiberChannelStorageVO, Timestamp> lastOpDate;
}
