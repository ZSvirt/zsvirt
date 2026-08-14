package org.zstack.storage.device.localRaid;

import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Create by weiwang at 2018/10/18
 */
@StaticMetamodel(RaidPhysicalDriveVO.class)
public class RaidPhysicalDriveVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> name;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> raidControllerUuid;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> description;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> raidLevel;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> wwn;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> serialNumber;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> deviceId;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> deviceModel;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> driveState;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> driveType;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, String> mediaType;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Integer> enclosureDeviceID;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Integer> slotNumber;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Integer> diskGroup;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Integer> rotationRate;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Long> size;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, LocateStatus> locateStatus;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Timestamp> createDate;
    public static volatile SingularAttribute<RaidPhysicalDriveVO, Timestamp> lastOpDate;
}
