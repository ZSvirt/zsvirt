package org.zstack.header.search;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.util.Date;

@StaticMetamodel(DeleteVO.class)
public class DeleteVO_ {
    public static volatile SingularAttribute<DeleteVO, Long> id;
    public static volatile SingularAttribute<DeleteVO, String> voName;
    public static volatile SingularAttribute<DeleteVO, String> uuid;
    public static volatile SingularAttribute<DeleteVO, String> foreignVOUuid;
    public static volatile SingularAttribute<DeleteVO, String> foreignVOName;
    public static volatile SingularAttribute<DeleteVO, String> foreignVOToDeleteUuid;
    public static volatile SingularAttribute<DeleteVO, String> foreignVOToDeleteName;
    public static volatile SingularAttribute<DeleteVO, Date> deletedDate;
}
