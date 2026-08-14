package org.zstack.header.description.resource;

import org.zstack.header.description.PackageDescriptionRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GlobalReadableResourceBuilder {
    private final List<Class<?>> readableResource = new ArrayList<>();

    public GlobalReadableResourceBuilder resources(Class<?>... clzs) {
        Collections.addAll(readableResource, clzs);
        return this;
    }

    public void build() {
        PackageDescriptionRegistry.readableResources.addAll(this.readableResource);
    }
}
