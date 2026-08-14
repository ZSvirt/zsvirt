package org.zstack.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by xing5 on 2016/5/18.
 */
public class VersionComparator {
    private String version;
    private List<Integer> elements = new ArrayList<Integer>();

    public VersionComparator(String version) {
        this.version = version;

        try {
            String[] els = version.split("\\.");
            for (String e : els) {
                elements.add(Integer.valueOf(e));
            }
        } catch (NumberFormatException e) {
            throw new RuntimeException(String.format("%s is not a valid version", version), e);
        }
    }

    public int compare(String ver) {
        VersionComparator c = new VersionComparator(ver);
        return compare(c);
    }

    public int compare(VersionComparator v) {
        List<Integer> him = new ArrayList<Integer>();
        him.addAll(v.elements);
        List<Integer> me = new ArrayList<Integer>();
        me.addAll(elements);

        if (me.size() > him.size()) {
            int diff = me.size() - him.size();
            for (int i=0; i<diff; i++) {
                him.add(0);
            }
        } else if (him.size() > me.size()) {
            int diff = him.size() - me.size();
            for (int i=0; i<diff; i++) {
                me.add(0);
            }
        }

        for (int i=0; i<me.size(); i++) {
            Integer m = me.get(i);
            Integer h = him.get(i);

            if (m.equals(h)) {
                continue;
            }

            return m > h ? 1 : -1;
        }

        return 0;
    }

    @Override
    public String toString() {
        return this.version;
    }
}
