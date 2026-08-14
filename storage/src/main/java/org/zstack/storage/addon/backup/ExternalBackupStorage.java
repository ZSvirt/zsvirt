package org.zstack.storage.addon.backup;

import org.zstack.core.Platform;
import org.zstack.header.core.Completion;
import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.image.CancelDownloadImageMsg;
import org.zstack.header.image.CancelDownloadImageReply;
import org.zstack.header.image.ImageInventory;
import org.zstack.header.storage.addon.ImageDescriptor;
import org.zstack.header.storage.addon.StorageHealthy;
import org.zstack.header.storage.addon.backup.BackupStorageController;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageInventory;
import org.zstack.header.storage.addon.backup.ExternalBackupStorageVO;
import org.zstack.header.storage.backup.*;
import org.zstack.storage.backup.BackupStorageBase;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

// ExternalBackupStorage is an Addon Backup Storage Instance (`absi').
//
// It is here to serve as a bridge of message based control flow to
// external Backup Storage Interface (`bsi').
public class ExternalBackupStorage extends BackupStorageBase {
    private static final CLogger logger = Utils.getLogger(ExternalBackupStorage.class);

    private final BackupStorageController controller;

    public ExternalBackupStorage(ExternalBackupStorageVO vo, BackupStorageController controller) {
        super(vo);
        this.controller = controller;
    }

    private ExternalBackupStorageVO getSelf() {
        return (ExternalBackupStorageVO) self;
    }

    protected BackupStorageInventory getSelfInventory() {
        return ExternalBackupStorageInventory.valueOf(getSelf());
    }

    @Override
    protected void handle(final DownloadImageMsg msg) {
        // TODO handler upload:// session
        controller.importImage(msg.getImageInventory(), new ReturnValueCompletion<ImageDescriptor>(msg) {
            final DownloadImageReply r = new DownloadImageReply();

            @Override
            public void success(ImageDescriptor d) {
                if (d.getInstallPath() == null) {
                    r.setError(Platform.operr("null installPath returned from driver: %s", controller.getIdentity()));
                } else {
                    r.setFormat(d.getFormat());
                    r.setInstallPath(d.getInstallPath());
                    r.setActualSize(d.getActualSize());
                    r.setSize(d.getSize());
                }
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(final CancelDownloadImageMsg msg) {
        controller.cancelImport(msg.getImageInventory(), new Completion(msg) {
            final CancelDownloadImageReply r = new CancelDownloadImageReply();

            @Override
            public void success() {
                bus.reply(msg, r);
            }

            @Override
            public void fail(ErrorCode errorCode) {
                r.setError(errorCode);
                bus.reply(msg, r);
            }
        });
    }

    @Override
    protected void handle(final DownloadVolumeMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void connectHook(boolean newAdded, Completion completion) {
        logger.info(String.format("connecting bs[%s], newly added: %s", getSelf().getName(), newAdded));
        controller.connect(newAdded, self.getUrl(), completion);
    }

    @Override
    protected void pingHook(final Completion completion) {
        logger.info(String.format("ping bs[%s]", getSelf().getName()));
        controller.reportHealthy(new ReturnValueCompletion<StorageHealthy>(completion) {
            @Override
            public void success(StorageHealthy healthy) {
                if (healthy == StorageHealthy.Ok) {
                    completion.success();
                    return;
                }

                completion.fail(Platform.operr("%s: health state: %s", getSelf().getIdentity(), healthy));
            }

            @Override
            public void fail(ErrorCode errorCode) {
                completion.fail(errorCode);
            }
        });
    }

    @Override
    public List<ImageInventory> scanImages() {
        return null;
    }

    @Override
    protected void handle(final DeleteBitsOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetImageSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(BackupStorageAskInstallPathMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(final SyncImageSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetLocalFileSizeOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(GetImageEncryptedOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(RestoreImagesBackupStorageMetadataToDatabaseMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }

    @Override
    protected void handle(CalculateImageHashOnBackupStorageMsg msg) {
        bus.replyErrorByMessageType(msg, "not supported");
    }
}
