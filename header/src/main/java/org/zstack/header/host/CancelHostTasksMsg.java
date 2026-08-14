package org.zstack.header.host;

import org.zstack.header.message.CancelMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by MaJin on 2019/7/22.
 *
 * cancel host task by specific api ID
 * searchedMnIds @param, hostUuids @param are no need to set.
 * do not retry by default.
 */
public class CancelHostTasksMsg extends CancelMessage {
    private List<String> searchedMnIds = new ArrayList<>();
    private List<String> hostUuids = new ArrayList<>();
    private Integer times;
    private Integer interval;

    public Integer getInterval() {
        return interval;
    }

    public void setInterval(Integer interval) {
        this.interval = interval;
    }

    public Integer getTimes() {
        return times;
    }

    public void setTimes(Integer times) {
        this.times = times;
    }

    public List<String> getSearchedMnIds() {
        return searchedMnIds;
    }

    public void setSearchedMnIds(List<String> searchedMnIds) {
        this.searchedMnIds = searchedMnIds;
    }

    public void addSearchedMnId(String mnId) {
        this.searchedMnIds.add(mnId);
    }

    public List<String> getHostUuids() {
        return hostUuids;
    }

    public void setHostUuids(List<String> hostUuids) {
        this.hostUuids = hostUuids;
    }

    public void addHostUuid(String hostUuid) {
        this.hostUuids.add(hostUuid);
    }
}
