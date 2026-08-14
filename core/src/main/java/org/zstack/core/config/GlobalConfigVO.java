package org.zstack.core.config;

import javax.persistence.*;

@Entity
@Table
public class GlobalConfigVO {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column
    private long id;
    
    @Column(updatable=false)
    private String name;
    
    @Column
    private String description;
    
    @Column
    private String category;
    
    @Column
    private String defaultValue;
    
    @Column
    private String value;

	public long getId() {
    	return id;
    }

	public void setId(long id) {
    	this.id = id;
    }

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

	public String getCategory() {
    	return category;
    }

	public void setCategory(String category) {
    	this.category = category;
    }

	public String getDefaultValue() {
    	return defaultValue;
    }

	public void setDefaultValue(String defaultValue) {
    	this.defaultValue = defaultValue;
    }

	public String getValue() {
    	return value;
    }

	public void setValue(String value) {
    	this.value = value;
    }
	
	public GlobalConfig toConfig() {
		 GlobalConfig c = new GlobalConfig();
		 c.setCategory(this.getCategory());
		 c.setDefaultValue(this.getDefaultValue());
		 c.setValue(this.getValue());
		 c.setName(this.getName());
		 c.setDescription(this.getDescription());
		 return c;
	}
	
	public GlobalConfigVO fromConfigSchema(org.zstack.core.config.schema.GlobalConfig.Config c) {
		GlobalConfigVO vo = this;
		vo.setCategory(c.getCategory());
		vo.setDefaultValue(c.getDefaultValue());
		vo.setDescription(c.getDescription());
		vo.setName(c.getName());
		vo.setValue(c.getValue());
		return vo;
	}
}
