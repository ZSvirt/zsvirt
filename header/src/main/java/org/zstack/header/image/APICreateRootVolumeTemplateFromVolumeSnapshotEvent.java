package org.zstack.header.image;

import org.zstack.header.errorcode.ErrorCode;
import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.rest.SDK;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
@RestResponse(fieldsTo = {"inventory", "failures=failuresOnBackupStorage"})
public class APICreateRootVolumeTemplateFromVolumeSnapshotEvent extends APIEvent {
    @SDK(sdkClassName = "CreateRootVolumeTemplateFromVolumeSnapshotFailure")
    public static class Failure {
        public String backupStorageUuid;
        public ErrorCode error;

        public String getBackupStorageUuid() {
            return backupStorageUuid;
        }

        public void setBackupStorageUuid(String backupStorageUuid) {
            this.backupStorageUuid = backupStorageUuid;
        }

        public ErrorCode getError() {
            return error;
        }

        public void setError(ErrorCode error) {
            this.error = error;
        }
    }

    private ImageInventory inventory;
    private List<Failure> failuresOnBackupStorage;

    public void addFailure(Failure failure) {
        if (failuresOnBackupStorage == null) {
            failuresOnBackupStorage = new ArrayList<>();
        }
        failuresOnBackupStorage.add(failure);
    }

    public List<Failure> getFailuresOnBackupStorage() {
        return failuresOnBackupStorage;
    }

    public void setFailuresOnBackupStorage(List<Failure> failuresOnBackupStorage) {
        this.failuresOnBackupStorage = failuresOnBackupStorage;
    }

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    public APICreateRootVolumeTemplateFromVolumeSnapshotEvent() {
        super(null);
    }

    public ImageInventory getInventory() {
        return inventory;
    }

    public void setInventory(ImageInventory inventory) {
        this.inventory = inventory;
    }
 
    public static APICreateRootVolumeTemplateFromVolumeSnapshotEvent __example__() {
        APICreateRootVolumeTemplateFromVolumeSnapshotEvent event = new APICreateRootVolumeTemplateFromVolumeSnapshotEvent();

        ImageInventory inv = new ImageInventory();
        inv.setUuid(uuid());

        ImageBackupStorageRefInventory ref = new ImageBackupStorageRefInventory();
        ref.setBackupStorageUuid(uuid());
        ref.setImageUuid(inv.getUuid());
        ref.setInstallPath("ceph://zs-images/0cd599ec519249489475112a058bb93a");
        ref.setStatus(ImageStatus.Ready.toString());

        inv.setName("My Root Volume Template");
        inv.setBackupStorageRefs(Collections.singletonList(ref));
        inv.setFormat(ImageConstant.RAW_FORMAT_STRING);
        inv.setMediaType(ImageConstant.ImageMediaType.RootVolumeTemplate.toString());
        inv.setPlatform(ImagePlatform.Linux.toString());

        event.setInventory(inv);

        return event;
    }

}
