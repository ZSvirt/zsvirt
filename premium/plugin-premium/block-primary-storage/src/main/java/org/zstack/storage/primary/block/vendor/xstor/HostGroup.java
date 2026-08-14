package org.zstack.storage.primary.block.vendor.xstor;

import javax.persistence.criteria.CriteriaBuilder;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/16 17:58
 */
public class HostGroup {
    public Integer id;
    public Integer key;
    public String name;
    public boolean mapped;
    public List<Integer> hosts;

    public Integer getId() {
        return id;
    }
}
