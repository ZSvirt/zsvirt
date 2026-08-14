package org.zstack.header.volume;

import org.zstack.header.message.DefaultTimeout;

import java.util.concurrent.TimeUnit;

/**
 * Created by MaJin on 2019/8/1.
 */

@DefaultTimeout(timeunit = TimeUnit.HOURS, value = 72)
public class CreateTemporaryDataVolumeFromVolumeTemplateMsg extends CreateDataVolumeFromVolumeTemplateMsg {
    private String originVolumeUuid;

    public String getOriginVolumeUuid() {
        return originVolumeUuid;
    }

    public void setOriginVolumeUuid(String originVolumeUuid) {
        this.originVolumeUuid = originVolumeUuid;
    }
}
