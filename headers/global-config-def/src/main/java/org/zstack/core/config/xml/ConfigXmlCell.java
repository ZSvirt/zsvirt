package org.zstack.core.config.xml;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

/**
 * One {@code <config>} element under {@code <globalConfig>}.
 * JAXB element names stay {@code config} / field tags; this is not {@code GlobalConfig}.
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "name",
        "category",
        "description",
        "type",
        "validatorRegularExpression",
        "defaultValue",
        "hidden"
})
public class ConfigXmlCell {
    @XmlElement(required = true)
    public String name;
    @XmlElement(required = true)
    public String category;
    @XmlElement(required = true)
    public String description;
    @XmlElement(required = true)
    public String type;
    @XmlElement(required = true)
    public String validatorRegularExpression;
    @XmlElement(required = true)
    public String defaultValue;
    public boolean hidden;
}
