package org.zstack.templateConfig.schema;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "config"
})
@XmlRootElement(name = "template")
public class GlobalConfigTemplate {
    protected List<GlobalConfigTemplate.Config> config;

    public List<GlobalConfigTemplate.Config> getConfig() {
        if (config == null) {
            config = new ArrayList<GlobalConfigTemplate.Config>();
        }
        return this.config;
    }

    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "uuid",
            "name",
            "description",
            "type",
            "validatorRegularExpression",
            "hidden"
    })
    public static class Config {
        protected String uuid;
        @XmlElement(required = true)
        protected String name;
        @XmlElement(required = true)
        protected String description;
        @XmlElement(required = true)
        protected String type;
        @XmlElement(required = true)
        protected String validatorRegularExpression;
        protected boolean hidden;

        public String getUuid() {
            return uuid;
        }

        public void setUuid(String uuid) {
            this.uuid = uuid;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public String getType() {
            return type;
        }

        public void setType(String value) {
            this.type = value;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String value) {
            this.description = value;
        }

        public String getValidatorRegularExpression() {
            return validatorRegularExpression;
        }

        public void setValidatorRegularExpression(String value) {
            this.validatorRegularExpression = value;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean value) {
            this.hidden = value;
        }

    }
}
