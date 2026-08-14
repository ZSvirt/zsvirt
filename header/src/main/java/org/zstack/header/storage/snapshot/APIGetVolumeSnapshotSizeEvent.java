package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

/**
 * Created by lining on 2019/5/14.
 */
@RestResponse(fieldsTo = {"all"})
public class APIGetVolumeSnapshotSizeEvent extends APIEvent {
    private Long size;
    private Long actualSize;

    public APIGetVolumeSnapshotSizeEvent() {
    }

    public APIGetVolumeSnapshotSizeEvent(String apiId) {
        super(apiId);
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public Long getActualSize() {
        return actualSize;
    }

    public void setActualSize(Long actualSize) {
        this.actualSize = actualSize;
    }

    public static APIGetVolumeSnapshotSizeEvent __example__() {
        APIGetVolumeSnapshotSizeEvent event = new APIGetVolumeSnapshotSizeEvent();
        event.setSize(SizeUnit.GIGABYTE.toByte(100));
        event.setActualSize(SizeUnit.GIGABYTE.toByte(50));
        return event;
    }

}
