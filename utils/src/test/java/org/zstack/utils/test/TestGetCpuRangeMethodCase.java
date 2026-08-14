package org.zstack.utils.test;

import org.junit.Test;
import org.zstack.utils.string.GetCpuRangeMethod;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TestGetCpuRangeMethodCase {
    String betweenCPURangeString = "0-5";
    String eachCPURangeString = "6,8,9,11";
    String allString = "0-9,^7,11";
    String errorString = "1,^1";
    Integer maxCPUID = 11;
    static List<Integer> betweenCPURange = IntStream.rangeClosed(0, 5).boxed().sorted().collect(Collectors.toList());
    static List<Integer> eachCPURange =Stream.of(6,8,9,11).sorted().collect(Collectors.toList());
    static List<Integer> all = Stream.of(0,1,2,3,4,5,6,8,9,11).sorted().collect(Collectors.toList());

    @Test
    public void testBetweenCPURange() {
        List<Integer> result = GetCpuRangeMethod.getCpuRange(betweenCPURangeString);
        result = result.stream().sorted().collect(Collectors.toList());
        assert betweenCPURange.size() == result.size();
        assert betweenCPURange.toString().equals(result.toString());
    }

    @Test
    public void testEachCPURange() {
        List<Integer> result = GetCpuRangeMethod.getCpuRange(eachCPURangeString);
        result = result.stream().sorted().collect(Collectors.toList());
        assert eachCPURange.size() == result.size();
        assert eachCPURange.toString().equals(result.toString());
    }

    @Test
    public void testGetCPURange() {
        List<Integer> result = GetCpuRangeMethod.getCpuRange(allString);
        result = result.stream().sorted().collect(Collectors.toList());

        assert all.size() == result.size();
        assert all.toString().equals(result.toString());
    }

    @Test
    public void testGetMaxCPUID() {
        int maxID = GetCpuRangeMethod.getMaxCpuIdInString(eachCPURangeString);
        assert maxID == maxCPUID;

        maxID = GetCpuRangeMethod.getMaxCpuIdInString(allString);
        assert maxID == maxCPUID;


        maxID = GetCpuRangeMethod.getMaxCpuIdInString(betweenCPURangeString);
        assert maxID == 5;

    }

    @Test
    public void testGetErrorCPURange() {
        List<Integer> result = GetCpuRangeMethod.getCpuRange(errorString);
        assert result.isEmpty();

        int maxID = GetCpuRangeMethod.getMaxCpuIdInString(errorString);
        assert maxID == -1;
    }
}
