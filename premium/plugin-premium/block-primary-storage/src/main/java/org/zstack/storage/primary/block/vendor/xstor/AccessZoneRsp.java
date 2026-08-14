package org.zstack.storage.primary.block.vendor.xstor;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/11 21:47
 */
public class AccessZoneRsp extends XStorServerResponse {
    public static String SubnetSinglePathMode = "SINGLE_PATH";
    public static String SubnetMultiPathMode = "MULTI_PATH";

    public class Subnet {
        public Integer access_zone_id;
        public Integer id;
        public String interface_type;
        public String description;
        public String ip_family;
        public String name;
        public Integer key;
        public Integer mtu;
        public List<String> network_interfaces = new ArrayList<>();
        public String subnet_gateway;
        public Integer subnet_mask;
        public String subnet_state;
        public String svip;
        public Integer version;
        public String path_mode;

        public String getSvip() {
            return svip;
        }

        public String getPathMode() {
            return path_mode;
        }
    }

    public class AccessZone {
        public String access_zone_state;
        public String name;
        public Integer id;
        public Integer key;
        public List<Subnet> subnets;

        public Integer getId() {
            return id;
        }

        public void setSubnets(List<Subnet> subnets) {
            this.subnets = subnets;
        }

        public List<Subnet> getSubnets() {
            return subnets;
        }
    }
    public class Result {
        public List<AccessZone> access_zones;
    }

    public Result result;

    public List<AccessZone> getAccessZones() {
        return result.access_zones;
    }
}