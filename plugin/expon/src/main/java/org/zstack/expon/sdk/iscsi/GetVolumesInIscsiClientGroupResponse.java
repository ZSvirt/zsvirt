package org.zstack.expon.sdk.iscsi;

import org.zstack.expon.sdk.ExponResponse;

import java.util.List;

public class GetVolumesInIscsiClientGroupResponse extends ExponResponse {
    private List<IscsiClientMappedLunModule> luns;
    private int count;

    public List<IscsiClientMappedLunModule> getLuns() {
        return luns;
    }

    public void setLuns(List<IscsiClientMappedLunModule> luns) {
        this.luns = luns;
    }

    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }
}
