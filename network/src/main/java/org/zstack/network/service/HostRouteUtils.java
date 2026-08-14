package org.zstack.network.service;

import org.apache.commons.lang.StringUtils;
import org.zstack.core.db.Q;
import org.zstack.header.network.l3.L3NetworkHostRouteVO;
import org.zstack.header.network.l3.L3NetworkHostRouteVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.gson.JSONObjectUtil;

import java.util.ArrayList;
import java.util.List;

public class HostRouteUtils {
    public static class HostRouteInfo {
        public String prefix;
        public String nexthop;

        @Override
        public String toString() {
            return "HostRouteInfo{" +
                    "prefix='" + prefix + '\'' +
                    ", nexthop='" + nexthop + '\'' +
                    '}';
        }
    }

    public static List<HostRouteInfo> getL3NetworkHostRoute(String l3NetworkUuid) {
        return getL3NetworkHostRoute(l3NetworkUuid, null);
    }

    public static List<HostRouteInfo> getL3NetworkHostRoute(String l3NetworkUuid, Integer ipVersion) {
        List<L3NetworkHostRouteVO> vos = Q.New(L3NetworkHostRouteVO.class).eq(L3NetworkHostRouteVO_.l3NetworkUuid, l3NetworkUuid).list();
        if (CollectionUtils.isEmpty(vos)) {
            return new ArrayList<>();
        }

        List<HostRouteInfo> res = new ArrayList<>();
        for (L3NetworkHostRouteVO vo : vos) {
            HostRouteInfo info = new HostRouteInfo();
            info.prefix = vo.getPrefix();
            info.nexthop = vo.getNexthop();
            res.add(info);
        }

        return res;
    }

    @SuppressWarnings({"unchecked"})
    public static List<HostRouteInfo> getHostRouteFromString(String hostRouteStr) {
        if (StringUtils.isEmpty(hostRouteStr)) {
            return new ArrayList<>();
        }

        try {
            return JSONObjectUtil.toCollection(hostRouteStr, ArrayList.class, HostRouteInfo.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
