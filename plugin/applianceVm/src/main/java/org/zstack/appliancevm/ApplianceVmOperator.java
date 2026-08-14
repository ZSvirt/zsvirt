package org.zstack.appliancevm;

import org.zstack.header.network.l3.L3NetworkInventory;
import org.zstack.header.network.l3.L3NetworkVO;
import org.zstack.utils.TagUtils;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ApplianceVmOperator {
    public static Map<String, Map<Integer, String>> parseStaticVipSystemTag(List<String> systemTags) {
        Map<String, Map<Integer, String>> ret = new HashMap<>();
        for (String sysTag : systemTags) {
            if (ApplianceVmSystemTags.APPLIANCEVM_STATIC_VIP.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_STATIC_VIP.getTagFormat(), sysTag);
                String staticVip = token.get(ApplianceVmSystemTags.APPLIANCEVM_STATIC_VIP_TOKEN);
                staticVip = IPv6NetworkUtils.ipv6TagValueToAddress(staticVip);
                String l3NetworkUUid = token.get(ApplianceVmSystemTags.APPLIANCEVM_STATIC_VIP_L3_TOKEN);

                if(!ret.containsKey(l3NetworkUUid)){
                    Map<Integer,String> versionIPMap = new HashMap<Integer, String>();
                    ret.put(l3NetworkUUid,versionIPMap);
                }

                if (NetworkUtils.isIpv4Address(staticVip)) {
                    ret.get(l3NetworkUUid).put(IPv6Constants.IPv4, staticVip);
                } else if (IPv6NetworkUtils.isIpv6Address(staticVip)) {
                    ret.get(l3NetworkUUid).put(IPv6Constants.IPv6, staticVip);
                }
            }
        }

        return ret;
    }

    public static Map<String, Map<Integer, String>> parseStaticIpSystemTag(List<String> systemTags) {
        Map<String, Map<Integer, String>> ret = new HashMap<>();
        for (String sysTag : systemTags) {
            if (ApplianceVmSystemTags.APPLIANCEVM_STATIC_IP.isMatch(sysTag)) {
                Map<String, String> token = TagUtils.parse(ApplianceVmSystemTags.APPLIANCEVM_STATIC_IP.getTagFormat(), sysTag);
                String staticIp = token.get(ApplianceVmSystemTags.APPLIANCEVM_STATIC_IP_TOKEN);
                staticIp = IPv6NetworkUtils.ipv6TagValueToAddress(staticIp);
                String l3NetworkUUid = token.get(ApplianceVmSystemTags.APPLIANCEVM_STATIC_IP_L3_TOKEN);

                if(!ret.containsKey(l3NetworkUUid)){
                    Map<Integer,String> versionIPMap = new HashMap<Integer, String>();
                    ret.put(l3NetworkUUid,versionIPMap);
                }

                if (NetworkUtils.isIpv4Address(staticIp)) {
                    ret.get(l3NetworkUUid).put(IPv6Constants.IPv4, staticIp);
                } else if (IPv6NetworkUtils.isIpv6Address(staticIp)) {
                    ret.get(l3NetworkUUid).put(IPv6Constants.IPv6, staticIp);
                }
            }
        }

        return ret;
    }
}
