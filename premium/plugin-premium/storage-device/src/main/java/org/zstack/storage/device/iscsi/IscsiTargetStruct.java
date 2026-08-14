package org.zstack.storage.device.iscsi;

import java.util.ArrayList;
import java.util.List;

/**
 * Create by weiwang at 2018/8/5
 */
public class IscsiTargetStruct {
    public String iqn;

    public List<IscsiLunStruct> iscsiLunStructList = new ArrayList<>();
}
