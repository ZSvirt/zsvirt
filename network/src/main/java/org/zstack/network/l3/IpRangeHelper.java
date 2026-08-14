package org.zstack.network.l3;

import javax.persistence.Tuple;
import javax.persistence.TupleElement;

import org.apache.commons.net.util.SubnetUtils;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.network.l3.*;
import org.zstack.utils.network.IPv6Constants;
import org.zstack.utils.network.IPv6NetworkUtils;
import org.zstack.utils.network.NetworkUtils;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.*;

import static org.zstack.utils.CollectionUtils.transform;


public class IpRangeHelper {
    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkInventory l3inv) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkInventory l3inv, int ipVersion) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == ipVersion).map(IpRangeInventory::getUuid).collect(Collectors.toList());
        if (uuids.isEmpty()) {
            return new ArrayList<>();
        }
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkVO vo) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getNormalIpRanges(L3NetworkVO vo, int ipVersion) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().filter(ipr -> ipr.getIpVersion() == ipVersion).map(IpRangeVO::getUuid).collect(Collectors.toList());
        if (uuids.isEmpty()) {
            return new ArrayList<>();
        }
        return NormalIpRangeInventory.valueOf1(Q.New(NormalIpRangeVO.class).in(NormalIpRangeVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkInventory l3inv) {
        if (l3inv.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = l3inv.getIpRanges().stream().map(IpRangeInventory::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(AddressPoolVO_.uuid, uuids).list());
    }

    public static List<IpRangeInventory> getAddressPools(L3NetworkVO vo) {
        if (vo.getIpRanges().isEmpty()) {
            return new ArrayList<>();
        }

        List<String> uuids = vo.getIpRanges().stream().map(IpRangeVO::getUuid).collect(Collectors.toList());
        return AddressPoolInventory.valueOf1(Q.New(AddressPoolVO.class).in(AddressPoolVO_.uuid, uuids).list());
    }

    public static void updateL3NetworkIpversion(IpRangeVO ipr) {
        updateL3NetworkIpversion(IpRangeInventory.valueOf(ipr));
    }

    public static void updateL3NetworkIpversion(IpRangeInventory ipr) {
        L3NetworkVO l3VO = Q.New(L3NetworkVO.class).eq(L3NetworkVO_.uuid, ipr.getL3NetworkUuid()).find();
        boolean ipv4 = false;
        boolean ipv6 = false;
        for (IpRangeVO vo : l3VO.getIpRanges()) {
            if (vo.getIpVersion() == IPv6Constants.IPv4) {
                ipv4 = true;
            } else if (vo.getIpVersion() == IPv6Constants.IPv6) {
                ipv6 = true;
            }
        }
        Integer ipVersion = IPv6Constants.NONE;
        if (ipv4 && ipv6) {
            ipVersion = IPv6Constants.DUAL_STACK;
        } else if (ipv4) {
            ipVersion = IPv6Constants.IPv4;
        } else if (ipv6) {
            ipVersion = IPv6Constants.IPv6;
        }

        if (!ipVersion.equals(l3VO.getIpVersion())) {
            SQL.New(L3NetworkVO.class).set(L3NetworkVO_.ipVersion, ipVersion).eq(L3NetworkVO_.uuid, l3VO.getUuid()).update();
        }
    }

    public static String getIpRangeType(String ipRangeUuid) {
        List<IpRangeVO> normalRanges = Q.New(NormalIpRangeVO.class).eq(NormalIpRangeVO_.uuid, ipRangeUuid).list();
        return normalRanges.size() > 0 ? IpRangeType.Normal.toString() : IpRangeType.AddressPool.toString();
    }

    public static List<BigInteger> getUsedIpInRange(String ipRangeUuid, int ipVersion) {
        Q q = Q.New(UsedIpVO.class).eq(UsedIpVO_.ipRangeUuid, ipRangeUuid);
        if (!getIpRangeType(ipRangeUuid).equals(IpRangeType.AddressPool.toString())) {
            String gateway = Q.New(IpRangeVO.class).select(IpRangeVO_.gateway)
                    .eq(IpRangeVO_.uuid, ipRangeUuid).findValue();
            q.notEq(UsedIpVO_.ip, gateway);
        }
        if (ipVersion == IPv6Constants.IPv4) {
            q.select(UsedIpVO_.ipInLong);
            List<Long> used = q.listValues();
            Collections.sort(used);
            return used.stream().distinct().map(l -> new BigInteger(String.valueOf(l))).collect(Collectors.toList());
        } else {
            q.select(UsedIpVO_.ip);
            List<String> used = q.listValues();
            return used.stream().distinct().map(IPv6NetworkUtils::getBigIntegerFromString).sorted().collect(Collectors.toList());
        }
    }

    public static List<FreeIpInventory> getFreeIp(final IpRangeVO ipr, int limit, String start) {
        List<String> usedIps = Q.New(UsedIpVO.class).select(UsedIpVO_.ip).eq(UsedIpVO_.ipRangeUuid, ipr.getUuid()).listValues();
        usedIps = usedIps.stream().distinct().collect(Collectors.toList());

        List<String> spareIps = new ArrayList<>();
        if (ipr.getIpVersion() == IPv6Constants.IPv6) {
            spareIps.addAll(NetworkUtils.getFreeIpv6InRange(ipr.getStartIp(), ipr.getEndIp(), usedIps, limit, start));
        } else {
            IpRangeVO cloneIpr = new IpRangeVO();
            cloneIpr.setStartIp(ipr.getStartIp());
            cloneIpr.setEndIp(ipr.getEndIp());
            cloneIpr.setNetmask(ipr.getNetmask());
            IpRangeHelper.stripNetworkAndBroadcastAddress(cloneIpr);
            spareIps.addAll(NetworkUtils.getFreeIpInRange(cloneIpr.getStartIp(), cloneIpr.getEndIp(), usedIps, limit, start));
        }

        return transform(spareIps, arg -> {
            FreeIpInventory f = new FreeIpInventory();
            f.setGateway(ipr.getGateway());
            f.setIp(arg);
            f.setNetmask(ipr.getNetmask());
            f.setIpRangeUuid(ipr.getUuid());
            return f;
        });
    }

    public static boolean stripNetworkAndBroadcastAddress(IpRangeVO ipr) {
        SubnetUtils sub = new SubnetUtils(ipr.getStartIp(), ipr.getNetmask());
        SubnetUtils.SubnetInfo info = sub.getInfo();
        boolean ret = true;

        if (ipr.getStartIp().equals(ipr.getEndIp())) {
            if (ipr.getStartIp().equals(info.getNetworkAddress()) || ipr.getEndIp().equals(info.getBroadcastAddress())) {
                ret = false;
            }
        }

        if (ipr.getStartIp().equals(info.getNetworkAddress())) {
            ipr.setStartIp(NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(ipr.getStartIp())+1));
        }
        if (ipr.getEndIp().equals(info.getBroadcastAddress())) {
            ipr.setEndIp(NetworkUtils.longToIpv4String(NetworkUtils.ipv4StringToLong(ipr.getEndIp())-1));
        }

        return ret;
    }

    public static List<Tuple> stripNetworkAndBroadcastAddress(List<Tuple> tuples) {
        List<Tuple> ret = new ArrayList<>();
        for (Tuple tuple : tuples) {
            String sip = tuple.get(0, String.class);
            String eip = tuple.get(1, String.class);
            String netmask = tuple.get(2, String.class);
            int ipVersion = tuple.get(3, Integer.class);
            if (ipVersion == IPv6Constants.IPv4) {
                IpRangeVO ipr = new IpRangeVO();
                ipr.setStartIp(sip);
                ipr.setEndIp(eip);
                ipr.setNetmask(netmask);
                stripNetworkAndBroadcastAddress(ipr);

                Tuple myTuple = new Tuple() {

                    private Map<Integer, Object> data = new HashMap<>();

                    {
                        data.put(0, ipr.getStartIp());
                        data.put(1, ipr.getEndIp());
                        data.put(2, tuple.get(2));
                        data.put(3, tuple.get(3));
                        data.put(4, tuple.get(4));
                    }

                    @Override
                    public <X> X get(TupleElement<X> tupleElement) {
                        return null;
                    }

                    @Override
                    public <X> X get(String s, Class<X> aClass) {
                        return null;
                    }

                    @Override
                    public Object get(String s) {
                        return null;
                    }

                    @Override
                    public <X> X get(int i, Class<X> aClass) {
                        return aClass.cast(data.get(i));
                    }

                    @Override
                    public Object get(int i) {
                        return data.get(i);
                    }

                    @Override
                    public Object[] toArray() {
                        return new Object[0];
                    }

                    @Override
                    public List<TupleElement<?>> getElements() {
                        return null;
                    }
                };

                ret.add(myTuple);
            } else {
                ret.add(tuple);
            }
        }
        return ret;
    }

    public static String getIpRangeUuid(String l3Uuid, String ip) {
        if (IPv6NetworkUtils.isIpv6Address(ip)) {
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class)
                    .eq(IpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv6).list();
            for (IpRangeVO ipr : ipRangeVOS) {
                if (IPv6NetworkUtils.isIpv6InRange(ip, ipr.getStartIp(), ipr.getEndIp())) {
                    return ipr.getUuid();
                }
            }
        } else if (NetworkUtils.isIpv4Address(ip)) {
            List<IpRangeVO> ipRangeVOS = Q.New(IpRangeVO.class)
                    .eq(IpRangeVO_.l3NetworkUuid, l3Uuid)
                    .eq(IpRangeVO_.ipVersion, IPv6Constants.IPv4).list();
            for (IpRangeVO ipr : ipRangeVOS) {
                if (NetworkUtils.isInRange(ip, ipr.getStartIp(), ipr.getEndIp())) {
                    return ipr.getUuid();
                }
            }
        }

        return null;
    }

    public static Boolean checkIpRangeConflict(UsedIpVO usedIp) {
        if (IpRangeHelper.getIpRangeUuid(usedIp.getL3NetworkUuid(), usedIp.getIp()) == null) {
            return Boolean.TRUE;
        }
        return Boolean.FALSE;
    }
}
