package org.zstack.header.volume;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;


@StaticMetamodel(VolumeAO.class)
public class VolumeAO_ extends ResourceVO_ {
    public static volatile SingularAttribute<VolumeAO, String> description;
    public static volatile SingularAttribute<VolumeAO, String> name;
    public static volatile SingularAttribute<VolumeAO, String> primaryStorageUuid;
    public static volatile SingularAttribute<VolumeAO, String> vmInstanceUuid;
    public static volatile SingularAttribute<VolumeAO, String> rootImageUuid;
    public static volatile SingularAttribute<VolumeAO, String> diskOfferingUuid;
    public static volatile SingularAttribute<VolumeAO, String> format;
    public static volatile SingularAttribute<VolumeAO, String> installPath;
    public static volatile SingularAttribute<VolumeAO, VolumeType> type;
    public static volatile SingularAttribute<VolumeAO, Long> size;
    public static volatile SingularAttribute<VolumeAO, Long> actualSize;
    public static volatile SingularAttribute<VolumeAO, Integer> deviceId;
    public static volatile SingularAttribute<VolumeAO, VolumeState> state;
    public static volatile SingularAttribute<VolumeAO, VolumeStatus> status;
    public static volatile SingularAttribute<VolumeAO, Timestamp> createDate;
    public static volatile SingularAttribute<VolumeAO, Timestamp> lastOpDate;
    public static volatile SingularAttribute<VolumeAO, Timestamp> lastDetachDate;
    public static volatile SingularAttribute<VolumeAO, Timestamp> lastAttachDate;
    public static volatile SingularAttribute<VolumeAO, String> lastVmInstanceUuid;
    public static volatile SingularAttribute<VolumeAO, Boolean> isShareable;
    public static volatile SingularAttribute<VolumeAO, String> volumeQos;
    public static volatile SingularAttribute<VolumeAO, String> protocol;
}
