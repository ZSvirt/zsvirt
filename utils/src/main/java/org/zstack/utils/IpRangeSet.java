package org.zstack.utils;

import com.google.common.collect.*;
import com.google.common.collect.RangeSet;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.net.util.SubnetUtils;
import org.zstack.utils.network.NetworkUtils;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class IpRangeSet {
    private static Pattern rangePattern = Pattern.compile("^([\\d.]+)(-[\\d.]+)?$");
    private static Pattern cidrPattern = Pattern.compile("^([\\d.]+\\/.+)$");
    private RangeSet<Long> rangeSet = TreeRangeSet.create();

    private static String IP_SET_SEPARATOR = ",";
    private static String IP_SET_INVERT_PREFIX = "^";

    private void closed(String origin){
        Matcher rangeMatcher = rangePattern.matcher(origin);
        Matcher cidrMatcher = cidrPattern.matcher(origin);
        if (rangeMatcher.matches()) {
            Long start = NetworkUtils.ipv4StringToLong(rangeMatcher.group(1));
            Long end = Optional.ofNullable(rangeMatcher.group(2)).map(it -> NetworkUtils.ipv4StringToLong(it.replaceFirst("-", ""))).orElse(start);
            rangeSet.add(Range.closed(start, end));
        } else if (cidrMatcher.matches() && NetworkUtils.isCidr(cidrMatcher.group(1))) {
            String cidr = cidrMatcher.group(1);
            SubnetUtils utils = new SubnetUtils(cidr);
            SubnetUtils.SubnetInfo subnet = utils.getInfo();
            Long start = NetworkUtils.ipv4StringToLong(subnet.getLowAddress());
            Long end = NetworkUtils.ipv4StringToLong(subnet.getHighAddress());
            rangeSet.add(Range.closed(start, end));
        } else {
            throw new IllegalArgumentException(String.format("illegal word[%s] for ip range", origin));
        }
    }

    private void remove(IpRangeSet exclude) {
       rangeSet.removeAll(exclude.rangeSet);
    }

    public boolean contains(Long ip) {
        return rangeSet.contains(ip);
    }

    public long size() {
        long size = 0L;
        for (Range<Long> range : rangeSet.asRanges()) {
            range = ContiguousSet.create(range, DiscreteDomain.longs()).range();
            size += (range.upperEndpoint() - range.lowerEndpoint() + 1);
        }
        return size;
    }

    public static IpRangeSet generateIpRangeSet(String word) {
        word = StringUtils.deleteWhitespace(word);
        String[] sets = word.split(IP_SET_SEPARATOR);
        IpRangeSet contain = new IpRangeSet();
        IpRangeSet exclude = new IpRangeSet();
        for (String set : sets) {
            if (set.startsWith(IP_SET_INVERT_PREFIX)) {
                exclude.closed(set.substring(1));
            } else {
                contain.closed(set);
            }
        }

        contain.remove(exclude);

        return contain;
    }

    public static RangeSet<Long> listAllRanges(String word) {
        IpRangeSet contain = generateIpRangeSet(word);

        long size = contain.size();
        if (size == 0) {
            throw new IllegalArgumentException(String.format("Invalid empty ipset [%s]", word));
        }

        return contain.rangeSet;
    }

    public static Set<String> listAllIps(String word, long limit) {
        IpRangeSet contain = generateIpRangeSet(word);

        long size = contain.size();
        if (size == 0) {
            throw new IllegalArgumentException(String.format("Invalid empty ipset [%s]", word));
        }

        if (size > limit) {
            throw new IllegalArgumentException(String.format("ip range length[%d] is too large, must less than %d", size, limit));
        }

        Set<String> results = new HashSet<>();
        for (Range<Long> range : contain.rangeSet.asRanges()) {
            range = ContiguousSet.create(range, DiscreteDomain.longs()).range();
            for (long i = range.lowerEndpoint(); i <= range.upperEndpoint() ; i++) {
                results.add(NetworkUtils.longToIpv4String(i));
            }
        }

        return results;
    }

    @Override
    public String toString(){
        return String.join(IP_SET_SEPARATOR, rangeSet.asRanges().stream().map(range -> {
            range = ContiguousSet.create(range, DiscreteDomain.longs()).range();
            long start = range.lowerEndpoint();
            long end = range.upperEndpoint();
            return start == end ? NetworkUtils.longToIpv4String(start) :
                    String.format("%s-%s", NetworkUtils.longToIpv4String(start),NetworkUtils.longToIpv4String(end));
        }).collect(Collectors.toList()));
    }
}