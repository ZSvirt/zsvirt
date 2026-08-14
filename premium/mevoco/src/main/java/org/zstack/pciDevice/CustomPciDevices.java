package org.zstack.pciDevice;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.List;

/**
 * Created by GuoYi on 11/19/19.
 */
@XmlRootElement(name="pciDevices")
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomPciDevices {
    @XmlElement(name="pciDevice")
    private List<CustomPciDevice> pciDevices;

    public List<CustomPciDevice> getPciDevices() {
        return pciDevices;
    }

    public void setPciDevices(List<CustomPciDevice> pciDevices) {
        this.pciDevices = pciDevices;
    }

    @XmlRootElement(name = "pciDevice")
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class CustomPciDevice {
        private String name;
        private String description;
        private String vendorId;
        private String deviceId;
        private String subVendorId;
        private String subDeviceId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getVendorId() {
            return vendorId;
        }

        public void setVendorId(String vendorId) {
            this.vendorId = vendorId;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public void setDeviceId(String deviceId) {
            this.deviceId = deviceId;
        }

        public String getSubVendorId() {
            return subVendorId;
        }

        public void setSubVendorId(String subVendorId) {
            this.subVendorId = subVendorId;
        }

        public String getSubDeviceId() {
            return subDeviceId;
        }

        public void setSubDeviceId(String subDeviceId) {
            this.subDeviceId = subDeviceId;
        }
    }
}
