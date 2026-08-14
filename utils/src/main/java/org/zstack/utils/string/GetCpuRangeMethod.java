package org.zstack.utils.string;

import java.util.*;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GetCpuRangeMethod {
    public static String CPU_SEPARATOR = ",";
    public static String CPU_BETWEEN_SEPARATOR = "-";
    public static String CPU_EXCLUDE_SEPARATOR = "^";

    public static List<Integer> getCpuRange(String CpuString) {
        Set<Integer> result = new HashSet<>();
        if (CpuString == null || CpuString.isEmpty()) {
            return new ArrayList<>(result);
        }

        String[] temp = CpuString.split(GetCpuRangeMethod.CPU_SEPARATOR);
        for (String s: temp) {
            if (s.contains(GetCpuRangeMethod.CPU_BETWEEN_SEPARATOR)) {
                String[] t = s.split(GetCpuRangeMethod.CPU_BETWEEN_SEPARATOR);
                result.addAll(IntStream.rangeClosed(Integer.parseInt(t[0]), Integer.parseInt(t[1])).boxed().collect(Collectors.toSet()));
            } else if (s.startsWith(GetCpuRangeMethod.CPU_EXCLUDE_SEPARATOR)) {
                result.remove(Integer.parseInt(s.substring(1)));
            } else {
                result.add(Integer.parseInt(s));
            }
        }
        return new ArrayList<>(result);
    }

    public static int getMaxCpuIdInString(String CpuString) {
        List<Integer> res = GetCpuRangeMethod.getCpuRange(CpuString);
        if (res.isEmpty()) {
            return -1;
        } else {
            return res.stream().sorted(Comparator.reverseOrder()).collect(Collectors.toList()).get(0);
        }
    }
}
