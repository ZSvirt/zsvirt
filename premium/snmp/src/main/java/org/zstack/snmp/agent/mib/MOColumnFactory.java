package org.zstack.snmp.agent.mib;

import org.snmp4j.agent.mo.MOAccessImpl;
import org.snmp4j.agent.mo.MOColumn;
import org.snmp4j.smi.OctetString;
import org.snmp4j.smi.SMIConstants;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @Author : jingwang
 * @create 2023/8/10 17:55
 */
public abstract class MOColumnFactory {
    private final SnmpGetHandler defaultHandler;

    private int idx = 0;
    private MOColumn[] columns;
    private final Map<String, Integer> metricNameToIdxMap = new HashMap<>();
    private final List<String> idxToMetricName = new ArrayList<>();
    private final Map<String, SnmpGetHandler> metricNameHandlerMap = new HashMap<>();

    protected MOColumnFactory() {
        this.defaultHandler = createDefaultHandler();
        registerColumns();
        initMOColumns();
    }

    protected SnmpGetHandler createDefaultHandler() {
        return new PrometheusSnmpGetHandler();
    }

    protected SnmpGetHandler infoMetricLabelHandler(String infoMetricName) {
        return new InfoMetricLabelHandler(infoMetricName);
    }

    private void initMOColumns() {
        columns = new MOColumn[idx];
        metricNameToIdxMap.forEach((metricName, idx) -> columns[idx] = createMOColumn(idx));
    }

    public MOColumn[] getColumns() {
        return columns;
    }

    public String getColumnName(int index) {
        if (index < 0 || index > idxToMetricName.size() - 1) {
            return null;
        }
        return idxToMetricName.get(index);
    }

    public int getColumnIndex(String columnName) {
        return metricNameToIdxMap.getOrDefault(columnName, -1);
    }

    public SnmpGetHandler getHandler(String columnName) {
        return metricNameHandlerMap.get(columnName);
    }

    protected MOColumn<OctetString> createMOColumn(int idx) {
        return new MOColumn<>(
                idx,
                SMIConstants.SYNTAX_OCTET_STRING,
                MOAccessImpl.ACCESS_READ_ONLY
        );
    }

    protected final void put(String metricName) {
        put(metricName, defaultHandler);
    }

    protected final void put(String metricName, SnmpGetHandler handler) {
        if (metricNameToIdxMap.containsKey(metricName)) {
            return;
        }
        metricNameToIdxMap.put(metricName, idx);
        metricNameHandlerMap.put(metricName, handler);
        idxToMetricName.add(metricName);
        idx++;
    }

    public abstract void registerColumns();
    public abstract String getType();
}
