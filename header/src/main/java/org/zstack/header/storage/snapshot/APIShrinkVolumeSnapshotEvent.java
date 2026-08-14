package org.zstack.header.storage.snapshot;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.utils.data.SizeUnit;

/**
 * @ Author : yh.w
 * @ Date   : Created in 13:25 2020/7/28
 */
@RestResponse(allTo = "shrinkResult")
public class APIShrinkVolumeSnapshotEvent extends APIEvent {

    public APIShrinkVolumeSnapshotEvent() {
        super(null);
    }

    public APIShrinkVolumeSnapshotEvent(String apiId) {
        super(apiId);
    }

    private ShrinkResult shrinkResult;

    public ShrinkResult getShrinkResult() {
        return shrinkResult;
    }

    public void setShrinkResult(ShrinkResult shrinkResult) {
        this.shrinkResult = shrinkResult;
    }

    public static APIShrinkVolumeSnapshotEvent __example__() {
        APIShrinkVolumeSnapshotEvent event = new APIShrinkVolumeSnapshotEvent();
        ShrinkResult result = new ShrinkResult();
        result.setOldSize(SizeUnit.GIGABYTE.toByte(2L));
        result.setSize(SizeUnit.GIGABYTE.toByte(1L));
        result.setDeltaSize(result.getOldSize() - result.getSize());
        event.setShrinkResult(result);
        return event;
    }
}
