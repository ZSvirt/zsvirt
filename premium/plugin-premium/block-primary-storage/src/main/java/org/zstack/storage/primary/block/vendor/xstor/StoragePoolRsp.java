package org.zstack.storage.primary.block.vendor.xstor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/11 17:48
 */
public class StoragePoolRsp extends XStorServerResponse{

    public class Layer {
        public long SUPER;
        public long MID;
        public long HIGH;
        public long LOW;
    }

    public class Layout {
        public Integer disk_parity_num;
        public Integer minNodeNum;
        public Integer node_parity_num;
        public Integer replica_num;
        public Integer stripe_width;

        public Integer getDisk_parity_num() {
            return disk_parity_num;
        }

        public Integer getMinNodeNum() {
            return minNodeNum;
        }

        public Integer getNode_parity_num() {
            return node_parity_num;
        }

        public Integer getReplica_num() {
            return replica_num;
        }

        public Integer getStripe_width() {
            return stripe_width;
        }
    }

    public static class StoragePool {
        public String authority_type;
        public String enable_data_cache;
        public Integer id;
        public Integer key;
        public Layer layerAvailBytes;
        public Layer layerTotalBytes;
        public Layer layerUsedRatio;
        public Layout layout;
        public String name;
        public long total_bytes;
        public long avail_bytes;
        public long used_bytes;
        public long used_ratio;

        public Integer getId() {
            return id;
        }

        public Layout getLayout() {
            return layout;
        }

        public long getAvail_bytes() {
            return avail_bytes;
        }

        public String getAuthorityType() {
            return authority_type;
        }

        public String getEnableDataCache() {
            return enable_data_cache;
        }

        public long getTotal_bytes() {
            return total_bytes;
        }

        public StoragePool() {}
    }
    public class Result {
        public Integer limit;
        public List<StoragePool> storage_pools = new ArrayList<>();
    }
    private Result result;

    public List<StoragePool> getStoragePools() {
        return result.storage_pools;
    }

}
