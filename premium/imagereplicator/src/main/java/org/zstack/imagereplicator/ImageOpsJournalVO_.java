package org.zstack.imagereplicator;

import javax.persistence.metamodel.SingularAttribute;
import javax.persistence.metamodel.StaticMetamodel;
import java.sql.Timestamp;

@StaticMetamodel(ImageOpsJournalVO.class)
public class ImageOpsJournalVO_ {
    public static volatile SingularAttribute<ImageOpsJournalVO, Long> id;
    public static volatile SingularAttribute<ImageOpsJournalVO, String> imageUuid;
    public static volatile SingularAttribute<ImageOpsJournalVO, String> backupStorageUuid;
    public static volatile SingularAttribute<ImageOpsJournalVO, ImageAction> action;
    public static volatile SingularAttribute<ImageOpsJournalVO, JournalType> type;
    public static volatile SingularAttribute<ImageOpsJournalVO, Timestamp> createDate;
    public static volatile SingularAttribute<ImageOpsJournalVO, Timestamp> lastOpDate;
}
