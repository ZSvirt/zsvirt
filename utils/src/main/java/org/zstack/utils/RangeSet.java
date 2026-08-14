package org.zstack.utils;

import java.util.*;
import java.util.stream.Collectors;

/**
 */
public class RangeSet {
    public static class Range {
        private long start;
        private long end;
        private Boolean isSystem;

        public Range(long start, long end) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
            this.isSystem = false;
        }

        public Range(long start, long end, Boolean isSystem) {
            this.start = Math.min(start, end);
            this.end = Math.max(start, end);
            this.isSystem = isSystem;
        }

        public Boolean getSystem() {
            return isSystem;
        }

        public void setSystem(Boolean system) {
            isSystem = system;
        }

        public long getStart() {
            return start;
        }

        public void setStart(long start) {
            this.start = start;
        }

        public long getEnd() {
            return end;
        }

        public void setEnd(long end) {
            this.end = end;
        }

        public boolean isOverlap(Range r) {
            return (start <= r.end && r.end <= end) || (start <= r.start && r.start <= end);
        }

        public boolean isConnected(Range r) {
            return end + 1 == r.getStart() || r.getEnd() + 1 == start;
        }

        public void merge(Range r) {
            DebugUtils.Assert(isOverlap(r) || isConnected(r), String.format("range %s is not overlap with %s", toString(), r.toString()));
            start = Math.min(start, r.start);
            end = Math.max(end, r.end);
        }

        public boolean is(long s, long e) {
            return start == s && end == e;
        }

        @Override
        public String toString() {
            return String.format("[%s, %s]", start, end);
        }
    }

    private List<Range> ranges = new ArrayList<Range>();

    public List<Range> getRanges() {
        return ranges;
    }

    public void setRanges(List<Range> ranges) {
        this.ranges = ranges;
    }

    public RangeSet open(long s, long e) {
        ranges.add(new Range(s+1, e-1));
        return this;
    }

    public RangeSet closed(long s, long e) {
        ranges.add(new Range(s, e));
        return this;
    }

    public RangeSet openClosed(long s, long e) {
        ranges.add(new Range(s+1, e));
        return this;
    }

    public RangeSet closedOpen(long s, long e) {
        ranges.add(new Range(s, e-1));
        return this;
    }

    public List<Range> merge() {
        List<Range> ret = new ArrayList<Range>();
        if (ranges.isEmpty()) {
            return ret;
        }

        sort();

        Range r = ranges.get(0);
        for (int i=1; i<ranges.size(); i++) {
            Range r1 = ranges.get(i);
            if (r.isOverlap(r1) || r.isConnected(r1)) {
                r.merge(r1);
            } else {
                ret.add(r);
                r = r1;
            }
        }
        ret.add(r);
        return ret;
    }

    public List<Range> mergeAndSort() {
        List<Range> ret = merge();
        sort();
        return ret;
    }

    public void sort(){
        Collections.sort(ranges, new Comparator<Range>() {
            @Override
            public int compare(Range o1, Range o2) {
                return Long.compare(o1.start, o2.start);
            }
        });
    }

    public List<String> sortAndToString() {
        List<String> strList = new ArrayList<String>();
        if (ranges.isEmpty()){
            return strList;
        }

        sort();

        Iterator<Range> it = ranges.iterator();
        while (it.hasNext()){
            Range range = it.next();
            for (long i = range.start; i <= range.end; i++){
                strList.add(Long.toString(i));
            }
        }

        return strList;
    }

    public static RangeSet valueOf(Collection<Long> numbers) {
        RangeSet results = new RangeSet();
        if (numbers.isEmpty()) {
            return results;
        }

        List<Long> asc = numbers.stream().sorted().distinct().collect(Collectors.toList());
        Long begin = asc.remove(0);
        Long end = begin;
        for (long n : asc) {
            long lastEnd = end;
            if (n != ++end) {
                results.ranges.add(new Range(begin, lastEnd));
                begin = n;
                end = n;
            }
        }
        results.ranges.add(new Range(begin, end));
        return results;
    }

    public Set<Long> values(){
        Set<Long> result = new HashSet<>();
        for (Range range : ranges) {
            for (long i = range.start; i <= range.end; i++) {
                result.add(i);
            }
        }
        return result;
    }
}
