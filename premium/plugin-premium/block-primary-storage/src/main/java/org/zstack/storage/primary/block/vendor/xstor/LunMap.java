package org.zstack.storage.primary.block.vendor.xstor;

import java.util.ArrayList;
import java.util.List;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/21 16:59
 */
public class LunMap {
    public Integer host_group_id;
    public Integer id;
    public Integer key;
    public Integer source_id;
    public String source_type;
    public Integer target_id;
    public String multi_path_mode;
    public List<Integer> target_ids = new ArrayList<>();
    public String host_group_name;
    public String lun_map_state;

    public Integer getId() {
        return id;
    }

    public Integer getHost_group_id() {
        return host_group_id;
    }

    public Integer getSource_id() {
        return source_id;
    }

    public String getSource_type() {
        return source_type;
    }

    public Integer getTargetId() {
        return target_id;
    }

    public String getHostGroupName() {
        return host_group_name;
    }

    public String getLunMapState() {
        return lun_map_state;
    }

    public List<Integer> getTarget_ids() {
        return target_ids;
    }
}
