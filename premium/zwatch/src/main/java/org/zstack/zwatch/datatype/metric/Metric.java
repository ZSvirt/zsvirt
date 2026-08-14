package org.zstack.zwatch.datatype.metric;

import org.zstack.zwatch.datatype.HostNetworkMetricFilter;
import org.zstack.zwatch.datatype.Unit;
import org.zstack.zwatch.datatype.UnitCovertRes;
import org.zstack.zwatch.datatype.ZWatchI18n;

import java.util.*;

public abstract class Metric {
    protected String name;
    protected String namespace;
    protected String description;
    protected Unit unit;
    protected List<String> labelNames = new ArrayList<>();
    protected boolean adminOnly = true;
    protected Map<String, HostNetworkMetricFilter> labelFilters = new HashMap<String, HostNetworkMetricFilter>();

    protected Metric(String name, Unit unit, Enum...labelNames) {
        this.name = name;
        this.unit = unit;
        for (Enum e : labelNames) {
            this.labelNames.add(e.toString());
        }
    }

    public abstract UnitCovertRes convertUnit(double origin);

    public boolean isAdminOnly() {
        return adminOnly;
    }

    public void setAdminOnly(boolean adminOnly) {
        this.adminOnly = adminOnly;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

    public List<String> getLabelNames() {
        return labelNames;
    }

    public void setLabelNames(List<String> labelNames) {
        this.labelNames = labelNames;
    }

    public boolean filterLabelValue(String label, Map value) {
        HostNetworkMetricFilter test = labelFilters.get(label);
        if (test == null) {
            return true;
        }

        return test.test(value);
    }

    public void addLabelFilter(String label, HostNetworkMetricFilter filter) {
        if (labelFilters.get(label) != null) {
            labelFilters.remove(label);
        }
        labelFilters.put(label, filter);
    }

    public boolean isFilterExisted(String filter) {
        return labelFilters.get(filter) != null;
    }

    public String getDescription() {
        return description;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
        this.description = ZWatchI18n.generateDescriptionFromName(namespace, getName());
    }
}
