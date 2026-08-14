package org.zstack.guesttools.advanced;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceConstant;

@RestRequest(
        path = "/vm-custom-specifications/{uuid}/actions",
        isAction = true,
        method = HttpMethod.PUT,
        responseClass = APIUpdateVmCustomSpecificationEvent.class
)
public class APIUpdateVmCustomSpecificationMsg extends APIMessage {
    @APIParam(resourceType = VmCustomSpecificationVO.class, scope = APIParam.SCOPE_MUST_OWNER, emptyString = false)
    private String uuid;

    @APIParam(required = false, maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(required = false, maxLength = 255)
    private String hostname;

    @APIParam(required = false, maxLength = 255, noTrim = true, validRegexValues = VmInstanceConstant.USER_VM_REGEX_PASSWORD)
    @NoLogging
    private String rootPassword;

    @APIParam(required = false)
    private Boolean generateSID;

    @APIParam(required = false, validEnums = VmCustomSpecificationDomainMode.class)
    private String domainMode;

    @APIParam(required = false, maxLength = 255)
    private String domainName;

    @APIParam(required = false, maxLength = 255)
    private String domainUsername;

    @APIParam(required = false, maxLength = 255, noTrim = true)
    @NoLogging
    private String domainPassword;

    @APIParam(required = false, maxLength = 255)
    private String organization;

    @GsonTransient
    @APINoSee
    private VmCustomSpecificationStruct vmCustomSpecification;

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

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

    public Boolean isGenerateSID() {
        return generateSID;
    }

    public void setGenerateSID(Boolean generateSID) {
        this.generateSID = generateSID;
    }

    public String getDomainMode() {
        return domainMode;
    }

    public void setDomainMode(String domainMode) {
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

    public VmCustomSpecificationStruct getVmCustomSpecification() {
        return vmCustomSpecification;
    }

    public void setVmCustomSpecification(VmCustomSpecificationStruct vmCustomSpecification) {
        this.vmCustomSpecification = vmCustomSpecification;
    }

    public static APIUpdateVmCustomSpecificationMsg __example__() {
        APIUpdateVmCustomSpecificationMsg msg = new APIUpdateVmCustomSpecificationMsg();
        msg.setUuid(uuid(VmCustomSpecificationVO.class));
        msg.setDescription("description");
        return msg;
    }
}
