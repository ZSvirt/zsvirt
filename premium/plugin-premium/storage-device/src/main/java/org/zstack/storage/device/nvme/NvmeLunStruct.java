package org.zstack.storage.device.nvme;

import java.util.Collections;
import java.util.List;

/**
 * Created by MaJin on 2022/8/10.
 */

public class NvmeLunStruct {
    public List<String> wwids;

    public String vendor;

    public String model;

    public String wwn;

    public String serial;

    public String type;

    public String path;

    public Long size;

    public String multipathDeviceUuid;

    public String nqn;

    public String transport;

    public String getWwid() {
        //NOTE(weiw): lvm-pv as a wwid is not stable, a simple wipefs will change it
        Collections.sort(wwids);
        for (String wwid : wwids) {
            if (wwid.contains("lvm-pv")) {
                continue;
            }
            return wwid;
        }
        return wwids.get(0);
    }

    private boolean isWwid(String wwid) {
        return true;
    }

    public boolean isValid() {
        if (wwids == null || wwids.isEmpty()) {
            return false;
        }

        wwids.removeIf(w -> !isWwid(w));
        return !wwids.isEmpty();
    }

    public List<String> getWwids() {
        return wwids;
    }

    public String getVendor() {
        return vendor;
    }

    public String getModel() {
        return model;
    }

    public String getWwn() {
        return wwn;
    }

    public String getSerial() {
        return serial;
    }

    public String getType() {
        return type;
    }

    public String getPath() {
        return path;
    }

    public Long getSize() {
        return size;
    }

    public String getMultipathDeviceUuid() {
        return multipathDeviceUuid;
    }

    public String getNqn() {
        return nqn;
    }

    public String getTransport() {
        return transport;
    }
}
