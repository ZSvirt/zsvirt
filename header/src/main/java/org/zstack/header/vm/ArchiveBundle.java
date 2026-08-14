package org.zstack.header.vm;

import java.util.ArrayList;
import java.util.List;

public class ArchiveBundle {
    private List<ResourceConfigBundle> resourceConfigBundles = new ArrayList<>();
    private List<SystemTagBundle> systemTagBundles = new ArrayList<>();

    public List<ResourceConfigBundle> getResourceConfigBundles() {
        return resourceConfigBundles;
    }

    public void setResourceConfigBundles(List<ResourceConfigBundle> resourceConfigBundles) {
        this.resourceConfigBundles = resourceConfigBundles;
    }

    public List<SystemTagBundle> getSystemTagBundles() {
        return systemTagBundles;
    }

    public void setSystemTagBundles(List<SystemTagBundle> systemTagBundles) {
        this.systemTagBundles = systemTagBundles;
    }
}
