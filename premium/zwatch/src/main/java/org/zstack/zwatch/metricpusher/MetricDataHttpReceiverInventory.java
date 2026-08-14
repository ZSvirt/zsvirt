package org.zstack.zwatch.metricpusher;

import org.zstack.core.Platform;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


@PythonClassInventory
@Inventory(mappingVOClass = MetricDataHttpReceiverVO.class, collectionValueOfMethod = "valueOf1")
public class MetricDataHttpReceiverInventory implements Serializable {
    private String uuid;
    private String name;
    private String url;
    private String description;
    private Timestamp createDate;
    private Timestamp lastOpDate;
    private ReceiverState state;

    protected MetricDataHttpReceiverInventory(MetricDataHttpReceiverVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setUrl(vo.getUrl());
        this.setDescription(vo.getDescription());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setState(vo.getState());
    }

    public static MetricDataHttpReceiverInventory valueOf(MetricDataHttpReceiverVO vo) {
        return new MetricDataHttpReceiverInventory(vo);
    }

    public static List<MetricDataHttpReceiverInventory> valueOf1(Collection<MetricDataHttpReceiverVO> vos) {
        List<MetricDataHttpReceiverInventory> invs = new ArrayList<MetricDataHttpReceiverInventory>(vos.size());
        for (MetricDataHttpReceiverVO vo : vos) {
            invs.add(MetricDataHttpReceiverInventory.valueOf(vo));
        }
        return invs;
    }

    public MetricDataHttpReceiverInventory() {
    }

    public static MetricDataHttpReceiverInventory __example__() {
        MetricDataHttpReceiverInventory ret = new MetricDataHttpReceiverInventory();
        ret.uuid = Platform.getUuid();
        ret.name = "CloudMonitor";
        ret.url = "http://127.0.0.1/xxx";
        ret.state = ReceiverState.Enabled;
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

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String $paramName) {
        url = $paramName;
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

    public ReceiverState getState() {
        return state;
    }

    public void setState(ReceiverState $paramName) {
        state = $paramName;
    }
}
