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
@XmlRootElement(name = "templateConfig")
public class TemplateConfig {
    protected List<TemplateConfig.Config> config;

    public List<TemplateConfig.Config> getConfig() {
        if (config == null) {
            config = new ArrayList<TemplateConfig.Config>();
        }
        return this.config;
    }


    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "", propOrder = {
            "templateName",
            "name",
            "category",
            "templateUuid",
            "type",
            "validatorRegularExpression",
            "defaultValue",
            "value",
            "hidden"
    })
    public static class Config {

        @XmlElement(required = true)
        protected String templateName;
        @XmlElement(required = true)
        protected String name;
        @XmlElement(required = true)
        protected String category;
        @XmlElement(required = true)
        protected String type;
        @XmlElement(required = true)
        protected String validatorRegularExpression;
        @XmlElement(required = true)
        protected String defaultValue;
        @XmlElement(required = true)
        protected String value;
        protected boolean hidden;

        protected String templateUuid;

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getName() {
            return name;
        }

        public void setName(String value) {
            this.name = value;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String value) {
            this.category = value;
        }

        public String getType() {return type; }

        public void setType(String type) { this.type = type; }

        public String getValidatorRegularExpression() {
            return validatorRegularExpression;
        }

        public void setValidatorRegularExpression(String value) {
            this.validatorRegularExpression = value;
        }

        public String getDefaultValue() {
            return defaultValue;
        }

        public void setDefaultValue(String value) {
            this.defaultValue = value;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        public String getTemplateUuid() {
            return templateUuid;
        }

        public void setTemplateUuid(String templateUuid) {
            this.templateUuid = templateUuid;
        }

        public boolean isHidden() {
            return hidden;
        }

        public void setHidden(boolean value) {
            this.hidden = value;
        }

    }
}
