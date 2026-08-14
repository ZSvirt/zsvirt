package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.message.NeedReplyMessage;
import org.zstack.utils.verify.Param;
import org.zstack.utils.verify.Verifiable;

/**
 * Created by GuoYi on 2018-12-26.
 */
public class AddPreconfigurationTemplateMsg extends NeedReplyMessage implements Verifiable {
    @Param(maxLength = 255)
    private String name;

    @Param(maxLength = 2048, required = false)
    private String description;

    @Param(maxLength = 64)
    private String distribution;

    @Param(validValues = {"kickstart", "preseed", "autoyast", "autoinstall"})
    private String type;

    @Param(maxLength = PreconfigurationConstant.contentMaxLength)
    private String content;

    private String accountUuid;

    private Boolean isPredefined = false;

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

    public String getDistribution() {
        return distribution;
    }

    public void setDistribution(String distribution) {
        this.distribution = distribution;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String accountUuid) {
        this.accountUuid = accountUuid;
    }

    public Boolean getPredefined() {
        return isPredefined;
    }

    public void setPredefined(Boolean predefined) {
        isPredefined = predefined;
    }

    public static AddPreconfigurationTemplateMsg valueOf(APIAddPreconfigurationTemplateMsg msg) {
        AddPreconfigurationTemplateMsg cmsg = new AddPreconfigurationTemplateMsg();
        cmsg.setName(msg.getName());
        cmsg.setDescription(msg.getDescription());
        cmsg.setDistribution(msg.getDistribution());
        cmsg.setType(msg.getType());
        cmsg.setContent(msg.getContent());
        cmsg.setAccountUuid(msg.getSession().getAccountUuid());
        return cmsg;
    }
}
