package org.zstack.network.service.vip;

import java.io.Serializable;

/**
 * Created by weiwang on 29/10/2017
 */
public class CompositePrimaryKeyForVipPeerL3NetworkRefVO implements Serializable {
    private String vipUuid;
    private String l3NetworkUuid;

    public String getVipUuid() {
        return vipUuid;
    }

    public void setVipUuid(String vipUuid) {
        this.vipUuid = vipUuid;
    }

    public String getL3NetworkUuid() {
        return l3NetworkUuid;
    }

    public void setL3NetworkUuid(String l3NetworkUuid) {
        this.l3NetworkUuid = l3NetworkUuid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CompositePrimaryKeyForVipPeerL3NetworkRefVO that = (CompositePrimaryKeyForVipPeerL3NetworkRefVO) o;

        if (!vipUuid.equals(that.vipUuid)) return false;
        return l3NetworkUuid.equals(that.l3NetworkUuid);
    }

    @Override
    public int hashCode() {
        int result = vipUuid.hashCode();
        result = 31 * result + l3NetworkUuid.hashCode();
        return result;
    }
}
