package org.zstack.guesttools.advanced;

import org.springframework.http.HttpMethod;
import org.zstack.header.configuration.VmCustomSpecificationDomainMode;
import org.zstack.header.configuration.VmCustomSpecificationStruct;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.message.GsonTransient;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.vm.VmInstanceConstant;

@RestRequest(
        path = "/vm-custom-specifications",
        method = HttpMethod.POST,
        responseClass = APICreateVmCustomSpecificationEvent.class,
        parameterName = "params"
)
public class APICreateVmCustomSpecificationMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255, emptyString = false)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(validEnums = ImagePlatform.class)
    private String platform;

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

    public static APICreateVmCustomSpecificationMsg __example__() {
        APICreateVmCustomSpecificationMsg msg = new APICreateVmCustomSpecificationMsg();
        msg.setName("custom-specification-for-vm-1");
        msg.setDescription("Test");
        msg.setPlatform(ImagePlatform.Windows.toString());
        msg.setHostname("vm-1");
        msg.setRootPassword("password");
        msg.setGenerateSID(true);
        msg.setDomainMode(VmCustomSpecificationDomainMode.Domain.toString());
        msg.setDomainName("zsv.test");
        msg.setDomainUsername("administrator");
        msg.setDomainPassword("password");
        msg.setOrganization("OU=zsv,DC=zsv,DC=test");

        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateVmCustomSpecificationEvent)rsp).getInventory().getUuid() : "", VmCustomSpecificationVO.class);
    }
}
