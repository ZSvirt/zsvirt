package org.zstack.sdk.zwatch.api;



public class MetricStruct  {

    public java.lang.String namespace;
    public void setNamespace(java.lang.String namespace) {
        this.namespace = namespace;
    }
    public java.lang.String getNamespace() {
        return this.namespace;
    }

    public java.lang.String name;
    public void setName(java.lang.String name) {
        this.name = name;
    }
    public java.lang.String getName() {
        return this.name;
    }

    public java.lang.String description;
    public void setDescription(java.lang.String description) {
        this.description = description;
    }
    public java.lang.String getDescription() {
        return this.description;
    }

    public java.util.List labelNames;
    public void setLabelNames(java.util.List labelNames) {
        this.labelNames = labelNames;
    }
    public java.util.List getLabelNames() {
        return this.labelNames;
    }

    public java.lang.String driver;
    public void setDriver(java.lang.String driver) {
        this.driver = driver;
    }
    public java.lang.String getDriver() {
        return this.driver;
    }

}
