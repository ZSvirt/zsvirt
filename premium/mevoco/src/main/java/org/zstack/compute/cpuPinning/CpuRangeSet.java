package org.zstack.compute.cpuPinning;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.RangeSet;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;

public class CpuRangeSet {
    private static Pattern rangePattern = Pattern.compile("^(\\d+)(-\\d+)?$");
    private RangeSet rangeSet = new RangeSet();

    private void closed(String origin){
        Matcher matcher = rangePattern.matcher(origin);
        if (matcher.matches()) {
            Long start = Long.valueOf(matcher.group(1));
            Long end = Long.valueOf(Optional.ofNullable(matcher.group(2)).orElse(start.toString()).replaceFirst("-", ""));
            rangeSet.closed(start, end);
        } else {
            throw new IllegalArgumentException(String.format("illegal word[%s] for range", origin));
        }
    }

    public static CpuRangeSet valueOf(String word){
        CpuRangeSet result = new CpuRangeSet();
        Set<Long> values = originValueOf(word);
        result.rangeSet = RangeSet.valueOf(values);
        return result;
    }

    public static CpuRangeSet valueOf(Collection<Long> numbers){
        CpuRangeSet result = new CpuRangeSet();
        result.rangeSet = RangeSet.valueOf(numbers);
        return result;
    }

    public static Set<Long> originValueOf(String word){
        String[] sets = word.split(CpuPinningConstant.CPU_SET_SEPARATOR);
        CpuRangeSet contain = new CpuRangeSet();
        CpuRangeSet exclude = new CpuRangeSet();
        for (String set : sets) {
            if (set.startsWith(CpuPinningConstant.CPU_SET_INVERT_PREFIX)) {
                exclude.closed(set.substring(1));
            } else {
                contain.closed(set);
            }
        }

        Set<Long> values = contain.rangeSet.values();
        values.removeAll(exclude.rangeSet.values());
        if (values.isEmpty()) {
            throw new OperationFailureException(argerr("Invalid cpuset [%s]", word));
        }

        return values;
    }

    @Override
    public String toString(){
        return String.join(CpuPinningConstant.CPU_SET_SEPARATOR, rangeSet.getRanges().stream().map(range ->
                range.getStart() == range.getEnd() ? String.valueOf(range.getStart()) :
                        String.format("%s-%s", range.getStart(), range.getEnd()))
                .collect(Collectors.toList()));
    }
}
