package org.zstack.header.description.resource;

import org.zstack.header.description.PackageDescriptionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class AttributeSupportResourceBuilder {
    private final List<Class<?>> attributeSupportResource = new ArrayList<>();

    public AttributeSupportResourceBuilder resources(Class<?>... clzs) {
        Collections.addAll(attributeSupportResource, clzs);
        return this;
    }

    public void build() {
        PackageDescriptionRegistry.attributeSupportResources.addAll(this.attributeSupportResource);
    }
}
