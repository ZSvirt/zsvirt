package org.zstack.header.baremetal.pxeserver;

import org.zstack.header.storage.backup.BackupStorageAO;
import org.zstack.header.vo.ResourceVO_;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

/**
 * Created by GuoYi on 4/20/17.
 */

@StaticMetamodel(BaremetalPxeServerVO.class)
public class BaremetalPxeServerVO_ extends ResourceVO_ {
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> zoneUuid;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> name;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> description;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> hostname;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> sshUsername;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> sshPassword;
    public static volatile SingularAttribute<BaremetalPxeServerVO, Integer> sshPort;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> storagePath;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> dhcpInterface;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> dhcpInterfaceAddress;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> dhcpRangeBegin;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> dhcpRangeEnd;
    public static volatile SingularAttribute<BaremetalPxeServerVO, String> dhcpRangeNetmask;
    public static volatile SingularAttribute<BackupStorageAO, Long> totalCapacity;
    public static volatile SingularAttribute<BackupStorageAO, Long> availableCapacity;
    public static volatile SingularAttribute<BaremetalPxeServerVO, BaremetalPxeServerState> state;
    public static volatile SingularAttribute<BaremetalPxeServerVO, BaremetalPxeServerStatus> status;
    public static volatile SingularAttribute<BaremetalPxeServerVO, Timestamp> createDate;
    public static volatile SingularAttribute<BaremetalPxeServerVO, Timestamp> lastOpDate;
}
