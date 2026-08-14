package org.zstack.guesttools;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(GuestToolsStateVO.class)
public class GuestToolsStateVO_ {
    public static volatile SingularAttribute<GuestToolsStateVO, String> vmInstanceUuid;
    public static volatile SingularAttribute<GuestToolsQgaState, String> qgaState;
    public static volatile SingularAttribute<GuestToolsZWatchState, String> zwatchState;
    public static volatile SingularAttribute<GuestToolsStateVO, String> version;
    public static volatile SingularAttribute<GuestToolsStateVO, String> platform;
    public static volatile SingularAttribute<GuestToolsStateVO, String> osType;
    public static volatile SingularAttribute<GuestToolsStateVO, Timestamp> createDate;
    public static volatile SingularAttribute<GuestToolsStateVO, Timestamp> lastOpDate;
}
