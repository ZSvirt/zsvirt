package org.zstack.header.configuration;

import org.zstack.header.log.NoLogging;
import org.zstack.header.rest.SDK;

import java.io.Serializable;

@PythonClassInventory
@SDK(sdkClassName = "VmCustomSpecificationStruct")
public class VmCustomSpecificationStruct implements Serializable {
    private String uuid;
    private String platform;
    private String hostname;
    @NoLogging
    private String rootPassword;
    private Boolean generateSID;
    private VmCustomSpecificationDomainMode domainMode;
    private String domainName;
    private String domainUsername;
    @NoLogging
    private String domainPassword;
    private String organization;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getRootPassword() {
        return rootPassword;
    }

    public void setRootPassword(String rootPassword) {
        this.rootPassword = rootPassword;
    }

    public Boolean getGenerateSID() {
        return generateSID;
    }

    public void setGenerateSID(Boolean generateSID) {
        this.generateSID = generateSID;
    }

    public VmCustomSpecificationDomainMode getDomainMode() {
        return domainMode;
    }

    public void setDomainMode(VmCustomSpecificationDomainMode domainMode) {
        this.domainMode = domainMode;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainUsername() {
        return domainUsername;
    }

    public void setDomainUsername(String domainUsername) {
        this.domainUsername = domainUsername;
    }

    public String getDomainPassword() {
        return domainPassword;
    }

    public void setDomainPassword(String domainPassword) {
        this.domainPassword = domainPassword;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }
}
