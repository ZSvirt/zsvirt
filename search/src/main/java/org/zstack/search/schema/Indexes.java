
package org.zstack.search.schema;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>anonymous complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="default.analyzer.name" type="{http://www.w3.org/2001/XMLSchema}string"/>
 *         &lt;element name="analyzerDef" type="{http://zstack.org/schema/zstack}analyzerDefType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="index" type="{http://zstack.org/schema/zstack}indexType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "defaultAnalyzerName",
    "analyzerDef",
    "index"
})
@XmlRootElement(name = "indexes")
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class Indexes {

    @XmlElement(name = "default.analyzer.name", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String defaultAnalyzerName;
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<AnalyzerDefType> analyzerDef;
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<IndexType> index;

    /**
     * 获取defaultAnalyzerName属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getDefaultAnalyzerName() {
        return defaultAnalyzerName;
    }

    /**
     * 设置defaultAnalyzerName属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setDefaultAnalyzerName(String value) {
        this.defaultAnalyzerName = value;
    }

    /**
     * Gets the value of the analyzerDef property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the analyzerDef property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnalyzerDef().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AnalyzerDefType }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<AnalyzerDefType> getAnalyzerDef() {
        if (analyzerDef == null) {
            analyzerDef = new ArrayList<AnalyzerDefType>();
        }
        return this.analyzerDef;
    }

    /**
     * Gets the value of the index property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the index property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIndex().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IndexType }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<IndexType> getIndex() {
        if (index == null) {
            index = new ArrayList<IndexType>();
        }
        return this.index;
    }

}
