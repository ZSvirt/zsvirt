package org.zstack.header.vm.additions;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(VmHostFileContentVO.class)
public class VmHostFileContentVO_ {
    public static volatile SingularAttribute<VmHostFileContentVO, String> uuid;
    public static volatile SingularAttribute<VmHostFileContentVO, byte[]> content;
    public static volatile SingularAttribute<VmHostFileContentVO, VmHostFileContentFormat> format;
    public static volatile SingularAttribute<VmHostFileContentVO, Timestamp> createDate;
    public static volatile SingularAttribute<VmHostFileContentVO, Timestamp> lastOpDate;
}
