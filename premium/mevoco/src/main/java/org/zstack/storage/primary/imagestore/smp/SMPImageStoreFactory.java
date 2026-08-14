package org.zstack.storage.primary.imagestore.smp;

import org.zstack.header.storage.primary.*;
import org.zstack.storage.backup.imagestore.CleanImageMetaOnPrimaryStorageMsg;
import org.zstack.storage.primary.smp.SMPConstants;

import java.util.Arrays;
import java.util.List;


/**
 * Created by david on 7/27/16.
 */
public class SMPImageStoreFactory implements PrimaryStorageExtensionFactory {
    @Override
    public String getPrimaryStorageType() {
        return SMPConstants.SMP_TYPE;
    }

    @Override
    public PrimaryStorage getPrimaryStorage(PrimaryStorageVO vo) {
        return new SMPImageStoreBackend(vo);
    }

    @Override
    public List<Class> getMessageClasses() {
        return Arrays.asList(CommitVolumeAsImageOnPrimaryStorageMsg.class,
                CommitVolumeAsImageMsg.class,
                SelectBackupStorageMsg.class,
                CleanImageMetaOnPrimaryStorageMsg.class);
    }
}
