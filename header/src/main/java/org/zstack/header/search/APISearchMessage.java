package org.zstack.header.search;

import org.zstack.header.message.APIMessage;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Deprecated
public abstract class APISearchMessage extends APIMessage {
    public static class NOVTriple {
        private String name;
        private String op;
        private String val;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public String getVal() {
            return val;
        }

        public void setVal(String val) {
            this.val = val;
        }
    }

    public static class NOLTriple {
        private String name;
        private String op;
        private List<String> vals;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getOp() {
            return op;
        }

        public void setOp(String op) {
            this.op = op;
        }

        public List<String> getVals() {
            if (vals == null) {
                vals = new ArrayList<String>();
            }
            return vals;
        }

        public void setVals(List<String> vals) {
            this.vals = vals;
        }
    }

    private Set<String> fields;
    private Set<NOVTriple> nameOpValueTriples;
    private Set<NOLTriple> nameOpListTriples;
    private int start;
    private long size;
    private String inventoryUuid;

    public Set<String> getFields() {
        if (fields == null) {
            fields = new HashSet<String>(0);
        }
        return fields;
    }

    public void setFields(Set<String> fields) {
        this.fields = fields;
    }

    public Set<NOVTriple> getNameOpValueTriples() {
        if (nameOpValueTriples == null) {
            nameOpValueTriples = new HashSet<NOVTriple>(0);
        }
        return nameOpValueTriples;
    }

    public void setNameOpValueTriples(Set<NOVTriple> nameOpValueTriples) {
        this.nameOpValueTriples = nameOpValueTriples;
    }

    public int getStart() {
        return start;
    }

    public void setStart(int start) {
        this.start = start;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public Set<NOLTriple> getNameOpListTriples() {
        if (nameOpListTriples == null) {
            nameOpListTriples = new HashSet<NOLTriple>();
        }
        return nameOpListTriples;
    }

    public void setNameOpListTriples(Set<NOLTriple> nameOpListTriples) {
        this.nameOpListTriples = nameOpListTriples;
    }

    public String getInventoryUuid() {
        return inventoryUuid;
    }

    public void setInventoryUuid(String inventoryUuid) {
        this.inventoryUuid = inventoryUuid;
    }
}
