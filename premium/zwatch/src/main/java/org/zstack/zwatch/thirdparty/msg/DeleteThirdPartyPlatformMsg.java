package org.zstack.zwatch.thirdparty.msg;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.zwatch.thirdparty.api.APIDeleteThirdpartyPlatformMsg;

public class DeleteThirdPartyPlatformMsg extends NeedReplyMessage implements ThirdpartyPlatformMsg {
    private String uuid;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String getThirdpartyPlatformUuid() {
        return getUuid();
    }

    public static DeleteThirdPartyPlatformMsg valueOf(APIDeleteThirdpartyPlatformMsg apiMsg) {
        DeleteThirdPartyPlatformMsg msg = new DeleteThirdPartyPlatformMsg();
        msg.setUuid(apiMsg.getUuid());
        return msg;
    }
}
