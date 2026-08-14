package org.zstack.compute.vm.numa;

import org.zstack.header.errorcode.OperationFailureException;
import org.zstack.utils.RangeSet;

import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.zstack.core.Platform.argerr;


public class VmNumaUtils {
    private static final Pattern rangePattern = Pattern.compile("^(\\d+)(-\\d+)?$");
    private final RangeSet rangeSet = new RangeSet();

    private void closed(String origin) {
        Matcher matcher = rangePattern.matcher(origin);
        if (matcher.matches()) {
            int start = Integer.parseInt(matcher.group(1));
            int end = Integer.parseInt(Optional.ofNullable(matcher.group(2))
                    .orElse(Integer.toString(start)).replaceFirst("-", ""));
            rangeSet.closed(start, end);
        } else {
            throw new IllegalArgumentException(String.format("illegal word[%s] for range", origin));
        }
    }

    public static Set<Integer> originValueOf(String word) {
        String[] sets = word.split(VmNumaConstant.CPU_SET_SEPARATOR);
        VmNumaUtils contain = new VmNumaUtils();
        VmNumaUtils exclude = new VmNumaUtils();
        for (String set : sets) {
            if (set.startsWith(VmNumaConstant.CPU_SET_INVERT_PREFIX)) {
                exclude.closed(set.substring(1));
            } else {
                contain.closed(set);
            }
        }

        Set<Integer> values = contain.rangeSet.values().stream().map(Long::intValue).collect(Collectors.toSet());
        values.removeAll(exclude.rangeSet.values().stream().map(Long::intValue).collect(Collectors.toSet()));
        if (values.isEmpty()) {
            throw new OperationFailureException(argerr("invalid cpu set [%s]", word));
        }
        return values;
    }
}
