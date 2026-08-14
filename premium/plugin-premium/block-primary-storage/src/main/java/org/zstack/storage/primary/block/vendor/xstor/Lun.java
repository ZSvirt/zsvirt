package org.zstack.storage.primary.block.vendor.xstor;

import org.apache.commons.lang.StringUtils;
import org.zstack.storage.primary.block.BlockScsiLunVO;

import java.util.*;

import static org.zstack.storage.primary.block.vendor.xstor.XStorDevice.BLOCK_RE_CLONE;
import static org.zstack.storage.primary.block.vendor.xstor.XStorDevice.BLOCK_RE_LUN;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/18 16:47
 */
public class Lun {
    final static private String LUN_SERIES_LUN = "LUN_SERIES_LUN";
    final static private String LUN_SERIES_CLONE_LUN = "LUN_SERIES_CLONELUN";
    public Integer accessDelay;
    public Integer access_zone_id;
    public String access_zone_name;
    public String authority_type;
    public Integer binded_target_id;
    public String type;
    public String data_state;
    public String root_type;
    public String lun_state;
    public long usedBytes;
    public Integer id;
    public Integer key;
    public String name;
    public String target_name;
    public List<String> target_names = new ArrayList<>();
    public long total_bytes;
    public String business_type;
    public Integer aglun_io_slice_mb;
    public String wwn;
    public List<LunMap> lunMaps;
    public LunsetCapacityInfo lunset_capacity_info;

    public class LunsetCapacityInfo {
        public long bare_unprovisioned_bytes;
        public long lun_bare_usedbytes;
        public long lun_usedbytes;
        public String data_state;

        public void setLunUsedBytes(long lun_usedbytes) {
            this.lun_usedbytes = lun_usedbytes;
        }

        public long getLunUsedBytes() {
            return lun_usedbytes;
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getWwn() {
        return wwn;
    }

    public void setWwn(String wwn) {
        this.wwn = wwn;
    }

    public String getTargetName() {
        if (StringUtils.isEmpty(target_name) && !target_names.isEmpty()) {
            return target_names.get(0);
        }
        return target_name;
    }

    public List<LunMap> getLunMaps() {
        return lunMaps;
    }

    public void setLunMaps(List<LunMap> lunMaps) {
        this.lunMaps = lunMaps;
    }

    public LunsetCapacityInfo getLunSetCapacityInfo() {
        return lunset_capacity_info;
    }

    public void setLunSetCapacityInfo(LunsetCapacityInfo lunsetCapacityInfo) {
        this.lunset_capacity_info = lunsetCapacityInfo;
    }

    public void setTotalBytes(long total_bytes) {
        this.total_bytes = total_bytes;
    }

    public long getTotalBytes() {
        return total_bytes;
    }

    public void setUsedBytes(long usedBytes) {
        this.usedBytes = usedBytes;
    }

    public long getUsedBytes() {
        return usedBytes;
    }

    public void setRootType(String root_type) {
        this.root_type = root_type;
    }

    public String getRootType() {
        return root_type;
    }

    public String getLun_state() {
        return lun_state;
    }

    public void setLun_state(String lun_state) {
        this.lun_state = lun_state;
    }

    public Boolean isReady() {
        return lun_state.equals("LUN_READY");
    }

    public String getType() {
        switch (type) {
            case LUN_SERIES_LUN:
                return BLOCK_RE_LUN;
            case LUN_SERIES_CLONE_LUN:
                return BLOCK_RE_CLONE;
            default:
                return null;
        }
    }

    public BlockScsiLunVO toBlockScsiLun() {
        BlockScsiLunVO blockScsiLunVO = new BlockScsiLunVO();

        blockScsiLunVO.setTarget(getTargetName());
        blockScsiLunVO.setId(id);
        blockScsiLunVO.setName(name);
        blockScsiLunVO.setWwn(wwn);
        blockScsiLunVO.setSize(total_bytes);
        if (getType() != null) {
            blockScsiLunVO.setLunType(getType());
        }
        blockScsiLunVO.setUsedSize(getLunSetCapacityInfo().getLunUsedBytes());
        List<LunMap> lunMaps = getLunMaps();
        if (!lunMaps.isEmpty()) {
            blockScsiLunVO.setLunMapId(lunMaps.get(0).getId());
        }
        return blockScsiLunVO;
    }
}
