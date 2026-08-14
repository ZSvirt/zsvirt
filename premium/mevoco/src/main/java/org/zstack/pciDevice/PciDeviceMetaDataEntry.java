package org.zstack.pciDevice;

import org.zstack.utils.DebugUtils;

import java.io.Serializable;

/**
 * Created by weiwang on 07/07/2017.
 */
public class PciDeviceMetaDataEntry implements Serializable {
    public enum PciDeviceMetaDataOperator {
        Equal,
        Unequal
    }

    private String key;
    private PciDeviceMetaDataOperator op;
    private String value;

    public PciDeviceMetaDataEntry(){
    }

    public PciDeviceMetaDataEntry(String key, PciDeviceMetaDataOperator op, String value){
        this.setKey(key);
        this.setOp(op);
        this.setValue(value);
    }

    public PciDeviceMetaDataEntry(String raw) {
        String pattern = String.format("\\w*:(%s|%s){1}:\\w*", PciDeviceMetaDataOperator.Equal, PciDeviceMetaDataOperator.Unequal);
        DebugUtils.Assert(raw.matches(pattern), String.format("wrong PciDeviceMetaDataEntry format [%s]", raw));

        this.setKey(raw.split(":")[0]);
        this.setOp(PciDeviceMetaDataOperator.valueOf(raw.split(":")[1]));
        this.setValue(raw.split(":")[2]);
    }

    public boolean matches(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PciDeviceMetaDataEntry)) return false;

        PciDeviceMetaDataEntry that = (PciDeviceMetaDataEntry) o;
        if (that.getOp().equals(PciDeviceMetaDataOperator.Equal)) {
            return !this.getKey().equals(that.getKey()) || this.getValue().equals(that.getValue());
        } else {
            return !this.getKey().equals(that.getKey()) || !this.getValue().equals(that.getValue());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o || o == null) return true;
        if (getClass() != o.getClass()) return false;

        PciDeviceMetaDataEntry that = (PciDeviceMetaDataEntry) o;

        if (key != null ? !key.equals(that.key) : that.key != null) return false;
        if (op != that.op) return false;
        return value != null ? value.equals(that.value) : that.value == null;
    }

    @Override
    public int hashCode() {
        int result = key != null ? key.hashCode() : 0;
        result = 31 * result + (op != null ? op.hashCode() : 0);
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return String.format("%s:%s:%s", this.getKey(), this.getOp(), this.getValue());
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public PciDeviceMetaDataOperator getOp() {
        return op;
    }

    public void setOp(PciDeviceMetaDataOperator op) {
        this.op = op;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
