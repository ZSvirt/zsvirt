package org.zstack.storage.device.iscsi;

import java.util.List;

/**
 * Create by weiwang at 2018/8/5
 */
public class IscsiLunStruct {
    public List<String> wwids;

    public String vendor;

    public String model;

    public String wwn;

    public String serial;
    
    public String hctl;

    public String type;

    public String path;

    public Long size;

    public String multipathDeviceUuid;
}
