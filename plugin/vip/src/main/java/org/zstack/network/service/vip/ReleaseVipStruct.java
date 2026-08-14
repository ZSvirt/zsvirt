package org.zstack.network.service.vip;

/**
 * Created by xing5 on 2016/11/20.
 */
public class ReleaseVipStruct {
    private boolean releasePeerL3Network;

    public boolean isReleasePeerL3Network() {
        return releasePeerL3Network;
    }

    public void setReleasePeerL3Network(boolean releasePeerL3Network) {
        this.releasePeerL3Network = releasePeerL3Network;
    }
}
