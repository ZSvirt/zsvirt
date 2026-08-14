package org.zstack.zwatch.metricpusher;

import org.zstack.core.Platform;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.query.ExpandedQueries;
import org.zstack.header.query.ExpandedQuery;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = MetricTemplateVO.class, collectionValueOfMethod = "valueOf1")
public class MetricTemplateInventory implements Serializable {
    private String uuid;
    private String receiverUuid;
    private String template;
    private String namespace;
    private String metricName;
    private String labelsJsonStr;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    protected MetricTemplateInventory(MetricTemplateVO vo) {
        this.setUuid(vo.getUuid());
        this.setReceiverUuid(vo.getReceiverUuid());
        this.setTemplate(vo.getTemplate());
        this.setNamespace(vo.getNamespace());
        this.setMetricName(vo.getMetricName());
        this.setLabelsJsonStr(vo.getLabelsJsonStr());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static MetricTemplateInventory valueOf(MetricTemplateVO vo) {
        return new MetricTemplateInventory(vo);
    }

    public static List<MetricTemplateInventory> valueOf1(Collection<MetricTemplateVO> vos) {
        List<MetricTemplateInventory> invs = new ArrayList<MetricTemplateInventory>(vos.size());
        for (MetricTemplateVO vo : vos) {
            invs.add(MetricTemplateInventory.valueOf(vo));
        }
        return invs;
    }

    public MetricTemplateInventory() {
    }

    public static MetricTemplateInventory __example__() {
        MetricTemplateInventory ret = new MetricTemplateInventory();
        ret.setUuid(Platform.getUuid());
        ret.setReceiverUuid(Platform.getUuid());
        ret.template = "{" +
                "  \"metricName\":\"${METRIC_NAME}\",\n" +
                "  \"regionId\":\"cn-shanghai\",\n" +
                "  \"instanceId\":\"${RESOURCE_UUID}\",\n" +
                "  \"resource\":\"vm/${RESOURCE_NAME}\",\n" +
                "  \"dimensions\":{\n" +
                "      \"instanceId\":\"${METRIC_LABLES.get('VMUuid')}\",\n" +
                "      \"device\":\"${METRIC_LABLES.get('DiskDeviceLetter')}\"\n" +
                "  },\n" +
                "  \"value\":${METRIC_VALUE},\n" +
                "  \"ts\":${METRIC_TIME}\n" +
                "}";
        ret.setLabelsJsonStr("['VMUuid=95e5885772de4f2bb3e05eba98a43cd0']");
        ret.setMetricName("DiskReadOps");
        ret.setNamespace("ZStack/VM");
        ret.createDate = new Timestamp(org.zstack.header.message.DocUtils.date);
        ret.lastOpDate = new Timestamp(org.zstack.header.message.DocUtils.date);
        return ret;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String $paramName) {
        uuid = $paramName;
    }

    public String getReceiverUuid() {
        return receiverUuid;
    }

    public void setReceiverUuid(String $paramName) {
        receiverUuid = $paramName;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String $paramName) {
        template = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
    }

    public String getMetricName() {
        return metricName;
    }

    public void setMetricName(String $paramName) {
        metricName = $paramName;
    }

    public String getLabelsJsonStr() {
        return labelsJsonStr;
    }

    public void setLabelsJsonStr(String $paramName) {
        labelsJsonStr = $paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String $paramName) {
        description = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp $paramName) {
        lastOpDate = $paramName;
    }
}
