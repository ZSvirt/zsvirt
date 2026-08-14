
package org.zstack.search.schema;

import java.util.ArrayList;
import java.util.List;
import javax.annotation.Generated;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>indexType complex type的 Java 类。
 * 
 * <p>以下模式片段指定包含在此类中的预期内容。
 * 
 * <pre>
 * &lt;complexType name="indexType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="prop" maxOccurs="unbounded" minOccurs="0">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="analyzer" type="{http://www.w3.org/2001/XMLSchema}string" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *       &lt;/sequence>
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *       &lt;attribute name="baseClass" use="required" type="{http://www.w3.org/2001/XMLSchema}boolean" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "indexType", propOrder = {
    "prop"
})
@Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
public class IndexType {

    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected List<IndexType.Prop> prop;
    @XmlAttribute(name = "name", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected String name;
    @XmlAttribute(name = "baseClass", required = true)
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    protected boolean baseClass;

    /**
     * Gets the value of the prop property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the prop property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProp().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link IndexType.Prop }
     * 
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public List<IndexType.Prop> getProp() {
        if (prop == null) {
            prop = new ArrayList<IndexType.Prop>();
        }
        return this.prop;
    }

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
     * 获取baseClass属性的值。
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public boolean isBaseClass() {
        return baseClass;
    }

    /**
     * 设置baseClass属性的值。
     * 
     */
    @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
    public void setBaseClass(boolean value) {
        this.baseClass = value;
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
     *       &lt;attribute name="analyzer" type="{http://www.w3.org/2001/XMLSchema}string" />
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
    public static class Prop {

        @XmlAttribute(name = "name", required = true)
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        protected String name;
        @XmlAttribute(name = "analyzer")
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        protected String analyzer;

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
         * 获取analyzer属性的值。
         * 
         * @return
         *     possible object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public String getAnalyzer() {
            return analyzer;
        }

        /**
         * 设置analyzer属性的值。
         * 
         * @param value
         *     allowed object is
         *     {@link String }
         *     
         */
        @Generated(value = "com.sun.tools.internal.xjc.Driver", date = "2020-11-19T07:25:57+08:00", comments = "JAXB RI v2.2.8-b130911.1802")
        public void setAnalyzer(String value) {
            this.analyzer = value;
        }

    }

}
