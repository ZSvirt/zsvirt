package org.zstack.pciDevice;

import org.zstack.utils.DebugUtils;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by weiwang on 07/07/2017.
 */
public class PciDeviceAddress implements Serializable {
    private String dbdf;

    private String domain;

    private String bus;

    private String slot;

    private String func;

    public PciDeviceAddress(String raw) {
        Map<String, String> parsed = parseFromRaw(raw);
        this.setDbdf(parsed.get("dbdf"));
        this.setDomain(parsed.get("domain"));
        this.setBus(parsed.get("bus"));
        this.setSlot(parsed.get("slot"));
        this.setFunc(parsed.get("func"));
    }

    private static Map<String, String> parseFromRaw(final String raw) {
        DebugUtils.Assert(raw.matches("(.*:){0,1}.*:.*\\..*"), String.format("error format for pci device metadata[%s]", raw));

        Map<String, String> parsed = new HashMap<>();
        String data = raw;
        if (raw.split(":").length == 3) {
            parsed.put("domain", raw.split(":")[0]);
            parsed.put("dbdf", raw);
            data = raw.split(":", 2)[1];
        } else {
            parsed.put("domain", "0000");
            parsed.put("dbdf", String.format("0000:%s", raw));
        }
        parsed.put("bus", data.split(":")[0]);
        parsed.put("slot", data.split(":")[1].split("\\.")[0]);
        parsed.put("func", data.split("\\.")[1]);
        return parsed;
    }

    public boolean dbdEquals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PciDeviceAddress)) return false;

        PciDeviceAddress that = (PciDeviceAddress) o;

        if (!domain.equals(that.domain)) return false;
        if (!bus.equals(that.bus)) return false;
        return slot.equals(that.slot);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || !(o instanceof PciDeviceAddress)) return false;

        PciDeviceAddress that = (PciDeviceAddress) o;

        return dbdf != null ? dbdf.equals(that.dbdf) : that.dbdf == null;
    }

    @Override
    public int hashCode() {
        return dbdf != null ? dbdf.hashCode() : 0;
    }

    @Override
    public String toString() {
        return this.getDbdf();
    }

    public PciDeviceAddress() {
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public String getBus() {
        return bus;
    }

    public void setBus(String bus) {
        this.bus = bus;
    }

    public String getSlot() {
        return slot;
    }

    public void setSlot(String slot) {
        this.slot = slot;
    }

    public String getFunc() {
        return func;
    }

    public void setFunc(String func) {
        this.func = func;
    }

    public String getDbdf() {
        return dbdf;
    }

    public void setDbdf(String dbdf) {
        this.dbdf = dbdf;
    }
}
