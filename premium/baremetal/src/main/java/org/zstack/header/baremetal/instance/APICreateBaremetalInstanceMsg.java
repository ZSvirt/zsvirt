package org.zstack.header.baremetal.instance;

import org.springframework.http.HttpMethod;
import org.zstack.header.baremetal.chassis.BaremetalChassisVO;
import org.zstack.header.baremetal.preconfiguration.PreconfigurationTemplateVO;
import org.zstack.header.image.ImagePlatform;
import org.zstack.header.image.ImageVO;
import org.zstack.header.log.NoLogging;
import org.zstack.header.message.APICreateMessage;
import org.zstack.header.message.APIEvent;
import org.zstack.header.message.APIMessage;
import org.zstack.header.message.APIParam;
import org.zstack.header.other.APIAuditor;
import org.zstack.header.rest.APINoSee;
import org.zstack.header.rest.RestRequest;
import org.zstack.header.tag.TagResourceType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by GuoYi on 7/4/18.
 */
@TagResourceType(BaremetalInstanceVO.class)
@RestRequest(
        path = "/baremetal/instances",
        method = HttpMethod.POST,
        responseClass = APICreateBaremetalInstanceEvent.class,
        parameterName = "params"
)
public class APICreateBaremetalInstanceMsg extends APICreateMessage implements APIAuditor {
    @APIParam(maxLength = 255)
    private String name;

    @APIParam(required = false, maxLength = 2048)
    private String description;

    @APIParam(resourceType = BaremetalChassisVO.class)
    private String chassisUuid;

    @APIParam(resourceType = ImageVO.class)
    private String imageUuid;

    @APIParam(resourceType = PreconfigurationTemplateVO.class, required = false)
    private String templateUuid;

    @APIParam(maxLength = 255, required = false)
    private String username = "root";

    @APIParam(maxLength = 255, password = true)
    @NoLogging
    private String password;

    @APIParam(required = false)
    private Map<String, String> nicCfgs;

    @APIParam(required = false)
    private Map<String, String> bondingCfgs;

    @APIParam(required = false)
    private Map<String, String> customConfigurations;

    @APIParam(required = false, validValues = {"InstantStart", "JustCreate"})
    private String strategy = BaremetalCreationStrategy.InstantStart.toString();

    @APINoSee
    private String nicInfo;

    @APIParam(required = false, validEnums = ImagePlatform.class)
    private String platform;

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

    public String getChassisUuid() {
        return chassisUuid;
    }

    public void setChassisUuid(String chassisUuid) {
        this.chassisUuid = chassisUuid;
    }

    public String getImageUuid() {
        return imageUuid;
    }

    public void setImageUuid(String imageUuid) {
        this.imageUuid = imageUuid;
    }

    public String getTemplateUuid() {
        return templateUuid;
    }

    public void setTemplateUuid(String templateUuid) {
        this.templateUuid = templateUuid;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, String> getNicCfgs() {
        return nicCfgs;
    }

    public void setNicCfgs(Map<String, String> nicCfgs) {
        this.nicCfgs = nicCfgs;
    }

    public Map<String, String> getBondingCfgs() {
        return bondingCfgs;
    }

    public void setBondingCfgs(Map<String, String> bondingCfgs) {
        this.bondingCfgs = bondingCfgs;
    }

    public Map<String, String> getCustomConfigurations() {
        return customConfigurations;
    }

    public void setCustomConfigurations(Map<String, String> customConfigurations) {
        this.customConfigurations = customConfigurations;
    }

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public String getNicInfo() {
        return nicInfo;
    }

    public void setNicInfo(String nicInfo) {
        this.nicInfo = nicInfo;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public static APICreateBaremetalInstanceMsg __example__() {
        APICreateBaremetalInstanceMsg msg = new APICreateBaremetalInstanceMsg();
        msg.setName("BM-1");
        msg.setDescription("This is a baremetal instance.");
        msg.setChassisUuid(uuid());
        msg.setImageUuid(uuid());
        msg.setTemplateUuid(uuid());
        msg.setUsername("root");
        msg.setPassword("password");
        Map<String, String> nicCfgs = new HashMap<>();
        nicCfgs.put("6c:b3:11:1b:0b:1e", uuid());
        nicCfgs.put("6c:b3:11:1b:0b:1f", uuid());
        msg.setNicCfgs(nicCfgs);
        Map<String, String> bondingCfgs = new HashMap<>();
        bondingCfgs.put(uuid(), uuid());
        msg.setBondingCfgs(bondingCfgs);
        Map<String, String> params = new HashMap<>();
        params.put("hostname", "localhost");
        params.put("keyboard", "en_US");
        msg.setCustomConfigurations(params);
        return msg;
    }

    @Override
    public Result audit(APIMessage msg, APIEvent rsp) {
        return new Result(rsp.isSuccess() ? ((APICreateBaremetalInstanceEvent)rsp).getInventory().getUuid() : "", BaremetalInstanceVO.class);
    }
}
