package org.zstack.core.config.xml;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * JAXB root for {@code globalConfig/*.xml} ({@code <globalConfig>}).
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "config"
})
@XmlRootElement(name = "globalConfig")
public class GlobalConfigXml {
    private static final JAXBContext CONTEXT;

    static {
        try {
            CONTEXT = JAXBContext.newInstance(GlobalConfigXml.class);
        } catch (JAXBException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @XmlElement(name = "config")
    protected List<ConfigXmlCell> config;

    public List<ConfigXmlCell> getConfig() {
        if (config == null) {
            config = new ArrayList<>();
        }
        return this.config;
    }

    public static GlobalConfigXml unmarshal(File file) throws JAXBException {
        Unmarshaller unmarshaller = CONTEXT.createUnmarshaller();
        return (GlobalConfigXml) unmarshaller.unmarshal(file);
    }
}
