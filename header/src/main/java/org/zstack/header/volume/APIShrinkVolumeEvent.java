package org.zstack.header.volume;

import org.zstack.header.message.APIEvent;
import org.zstack.header.rest.RestResponse;
import org.zstack.header.storage.snapshot.ShrinkResult;
import org.zstack.utils.data.SizeUnit;

@RestResponse(allTo = "shrinkResult")
public class APIShrinkVolumeEvent extends APIEvent {
    private ShrinkResult shrinkResult;

    public APIShrinkVolumeEvent() {
    }

    public APIShrinkVolumeEvent(String apiId) {
        super(apiId);
    }

    public ShrinkResult getShrinkResult() {
        return shrinkResult;
    }

    public void setShrinkResult(ShrinkResult shrinkResult) {
        this.shrinkResult = shrinkResult;
    }

    public static APIShrinkVolumeEvent __example__() {
        APIShrinkVolumeEvent event = new APIShrinkVolumeEvent();
        ShrinkResult result = new ShrinkResult();
        result.setOldSize(SizeUnit.GIGABYTE.toByte(5L));
        result.setSize(SizeUnit.GIGABYTE.toByte(2L));
        result.setDeltaSize(result.getOldSize() - result.getSize());
        event.setShrinkResult(result);
        return event;
    }
}
