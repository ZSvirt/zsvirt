package org.zstack.core.config.schema;

import javax.xml.bind.annotation.*;
import java.io.Serializable;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "osInfo"
})
@XmlRootElement(name = "character")
public class GuestOsCharacter {
    protected List<GuestOsCharacter.Config> osInfo;

    public List<Config> getOsInfo() {
        return osInfo;
    }

    public void setOsInfo(List<Config> osInfo) {
        this.osInfo = osInfo;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "architecture",
            "platform",
            "osRelease",
            "acpi",
            "x2apic",
            "cpuModel",
            "nicDriver",
            "hygonTag",
    })
    public static class Config implements Serializable {
        @XmlElement(required = true)
        protected String architecture;

        @XmlElement(required = true)
        protected String platform;

        @XmlElement(required = true)
        protected String osRelease;

        @XmlElement(required = false)
        protected Boolean acpi;

        @XmlElement(required = false)
        protected Boolean hygonTag;

        @XmlElement(required = false)
        protected Boolean x2apic;

        @XmlElement(required = false)
        protected String cpuModel;

        @XmlElement(required = false)
        protected String nicDriver;

        public String getPlatform() {
            return platform;
        }

        public void setPlatform(String platform) {
            this.platform = platform;
        }

        public String getOsRelease() {
            return osRelease;
        }

        public void setOsRelease(String osRelease) {
            this.osRelease = osRelease;
        }

        public Boolean getAcpi() {
            return acpi;
        }

        public Boolean getX2apic() {
            return x2apic;
        }

        public void setAcpi(Boolean acpi) {
            this.acpi = acpi;
        }

        public void setX2apic(Boolean x2apic) {
            this.x2apic = x2apic;
        }

        public Boolean getHygonTag() {
            return hygonTag;
        }

        public void setHygonTag(Boolean hygonTag) {
            this.hygonTag = hygonTag;
        }

        public String getArchitecture() {
            return architecture;
        }

        public void setArchitecture(String architecture) {
            this.architecture = architecture;
        }

        public String getCpuModel() {
            return cpuModel;
        }

        public void setCpuModel(String cpuModel) {
            this.cpuModel = cpuModel;
        }

        public String getNicDriver() {
            return nicDriver;
        }

        public void setNicDriver(String nicDriver) {
            this.nicDriver = nicDriver;
        }
    }
}
