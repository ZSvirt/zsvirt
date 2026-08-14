package org.zstack.storage.primary.block.vendor.xstor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/16 19:11
 */
public class Host {
    public Integer id;

    public Integer key;

    public Integer host_group_id;

    public String host_group_name;

    public List<Integer> initiatorIds = new ArrayList<>();

    public String name;

    public String os_type;

    public String state;

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getKey() {
        return key;
    }

    public Integer getHostGroupId() {
        return host_group_id;
    }

    public List<Integer> getInitiatorIds() {
        if (initiatorIds == null) {
            return Collections.EMPTY_LIST;
        }
        return initiatorIds;
    }

    public String getHostGroupName() {
        return host_group_name;
    }

    public String getOSType() {
        return os_type;
    }

    public String getState() {
        return state;
    }
}
