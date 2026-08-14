
package org.zstack.search.schema;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>filterType complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
 * <pre>
 * &lt;complexType name="filterType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="param" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="factory" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "filterType", propOrder = {
    "param"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class FilterType {

    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<FilterType.Param> param;
    @XmlAttribute(name = "factory", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String factory;

    /**
     * Gets the value of the param property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the param property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getParam().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link FilterType.Param }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<FilterType.Param> getParam() {
        if (param == null) {
            param = new ArrayList<FilterType.Param>();
        }
        return this.param;
    }

    /**
     * 获取factory属性的值。
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public String getFactory() {
        return factory;
    }

    /**
     * 设置factory属性的值。
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setFactory(String value) {
        this.factory = value;
    }


    /**
     * <p>anonymous complex type的 Java 类。
     * 
     * <p>以下模式片段指定包含在此类中的预期内容。
     * 
     * <pre>
     * &lt;complexType>
     *   &lt;complexContent>
     *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
     *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *       &lt;attribute name="value" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
     *     &lt;/restriction>
     *   &lt;/complexContent>
     * &lt;/complexType>
     * </pre>
     * 
     * 
     */
    @XmlAccessorType(XmlAccessType.FIELD)
    @XmlType(name = "")
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public static class Param {

        @XmlAttribute(name = "name", required = true)
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        protected String name;
        @XmlAttribute(name = "value", required = true)
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        protected String value;

        /**
         * 获取name属性的值。
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public String getName() {
            return name;
        }

        /**
         * 设置name属性的值。
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public void setName(String value) {
            this.name = value;
        }

        /**
         * 获取value属性的值。
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public String getValue() {
            return value;
        }

        /**
         * 设置value属性的值。
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public void setValue(String value) {
            this.value = value;
        }

    }

}
