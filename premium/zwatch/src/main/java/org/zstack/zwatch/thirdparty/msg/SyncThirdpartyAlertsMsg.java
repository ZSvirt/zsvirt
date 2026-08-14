package org.zstack.zwatch.thirdparty.msg;

import org.zstack.header.message.NeedReplyMessage;

/**
 * Created by yaoning.li on 2020/7/15.
 */
public class SyncThirdpartyAlertsMsg extends NeedReplyMessage implements ThirdpartyPlatformMsg {
    private String platformUuid;

    @Override
    public String getThirdpartyPlatformUuid() {
        return platformUuid;
    }

    public String getPlatformUuid() {
        return platformUuid;
    }

    public void setPlatformUuid(String platformUuid) {
        this.platformUuid = platformUuid;
    }
}
