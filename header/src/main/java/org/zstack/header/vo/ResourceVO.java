package org.zstack.header.vo;

import org.zstack.header.exception.CloudRuntimeException;
import org.zstack.utils.FieldUtils;

import javax.persistence.*;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by xing5 on 2017/4/29.
 */
@Entity
@Table
@Inheritance(strategy = InheritanceType.JOINED)
public class ResourceVO {
    @Id
    @Column
    @Index
    protected String uuid;

    @Column
    private String resourceName;

    @Column
    private String resourceType;

    @Column
    private String concreteResourceType;

    private static Map<Class, Field> nameFields = new ConcurrentHashMap<>();

    public ResourceVO() {
    }

    public ResourceVO(Object[] objs) {
        uuid = (String) objs[0];
        resourceName = (String) objs[1];
        resourceType = (String) objs[2];
    }

    private Field getNameField() {
        Field f = nameFields.get(getClass());
        if (f != null) {
            return f;
        }

        ResourceAttributes at = getClass().getAnnotation(ResourceAttributes.class);
        f = FieldUtils.getField(at == null ? "name" : at.nameField(), getClass());
        if (f != null) {
            f.setAccessible(true);
            nameFields.put(getClass(), f);
        }

        return f;
    }

    public boolean hasNameField() {
        return getNameField() != null;
    }

    public String getValueOfNameField() {
        try {
            String name = null;
            Field nameField = getNameField();
            if (nameField != null) {
                name = (String) nameField.get(this);
            } else if (this.getResourceName() != null) {
                name = this.getResourceName();
            }

            return name;
        } catch (IllegalAccessException e) {
            throw new CloudRuntimeException(e);
        }
    }

    @PrePersist
    private void prePersist() {
        resourceType = ResourceTypeMetadata.getBaseResourceTypeFromConcreteType(getClass()).getSimpleName();
        concreteResourceType = getClass().getName();
        resourceName = getValueOfNameField();
    }

    public String getConcreteResourceType() {
        return concreteResourceType;
    }

    public void setConcreteResourceType(String concreteResourceType) {
        this.concreteResourceType = concreteResourceType;
    }

    public String getResourceName() {
        return resourceName;
    }

    public void setResourceName(String resourceName) {
        this.resourceName = resourceName;
    }

    public String getResourceType() {
        return resourceType;
    }

    void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
