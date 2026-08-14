package org.zstack.pciDevice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by weiwang on 07/07/2017.
 */
public class PciDeviceMetaData implements Serializable {
    private String metaData;

    private List<PciDeviceMetaDataEntry> metaDataEntries;

    public PciDeviceMetaData(List<PciDeviceMetaDataEntry> map) {
        if (map != null) {
            setMetaData(serialize(map));
            setMetaDataEntries(map);
        }
    }

    public PciDeviceMetaData(String string) {
        if (string != null) {
            setMetaDataEntries(deserialize(string));
            setMetaData(string);
        }
    }

    public String serialize(List<PciDeviceMetaDataEntry> list) {
        if (list == null) {
            return null;
        }

        // Note(WeiW): Avoid the first ";" at result so we split at last
        return list.stream().map((PciDeviceMetaDataEntry -> PciDeviceMetaDataEntry.toString()))
                .reduce("", (e1, e2) -> String.format("%s;%s", e1, e2)).split(";", 2)[1];
    }

    public List<PciDeviceMetaDataEntry> deserialize(String string) {
        List<PciDeviceMetaDataEntry> result = new ArrayList<>();

        for (String s : string.split(";")) {
            result.add(new PciDeviceMetaDataEntry(s));
        }

        return result;
    }

    public boolean matches(Object o) {
        if (this == o || o == null) return true;
        if (!(o instanceof PciDeviceMetaData)) return false;

        PciDeviceMetaData that = (PciDeviceMetaData) o;
        if (that.metaDataEntries == null) return true;
        boolean result = true;

        for (PciDeviceMetaDataEntry thatEntry : that.getMetaDataEntries()) {
            if (this.metaDataEntries == null && thatEntry.getOp().equals(PciDeviceMetaDataEntry.PciDeviceMetaDataOperator.Equal)) {
                result = false;
                break;
            }
            if (this.metaDataEntries == null && thatEntry.getOp().equals(PciDeviceMetaDataEntry.PciDeviceMetaDataOperator.Unequal)) {
                result = true;
                continue;
            }
            result = result && this.getMetaDataEntries().stream()
                    .map((thisEntry -> thisEntry.matches(thatEntry)))
                    .reduce(true, (left, right) -> left && right);
        }
        return result;
    }

    @Override
    public String toString() {
        return this.getMetaData();
    }

    public PciDeviceMetaData() {
    }

    public String getMetaData() {
        return metaData;
    }

    public void setMetaData(String metaData) {
        this.metaData = metaData;
    }

    public List<PciDeviceMetaDataEntry> getMetaDataEntries() {
        return metaDataEntries;
    }

    public void setMetaDataEntries(List<PciDeviceMetaDataEntry> metaDataEntries) {
        this.metaDataEntries = metaDataEntries;
    }
}
