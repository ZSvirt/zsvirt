package org.zstack.zwatch.migratedb;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;
import org.zstack.zwatch.datatype.EventData;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@PythonClassInventory
@Inventory(mappingVOClass = EventRecordsVO.class, collectionValueOfMethod = "valueOf1")
public class EventRecordsInventory implements Serializable {
    private long id;
    private long createTime;
    private String namespace;
    private String name;
    private String emergencyLevel;
    private String resourceId;
    private String error;
    private String dataUuid;
    private String accountUuid;
    private String subscriptionUuid;
    private Boolean readStatus;
    private String operatorAccountUuid;
    private String labels;

    protected EventRecordsInventory(EventRecordsVO vo) {
        this.setId(vo.getId());
        this.setCreateTime(vo.getCreateTime());
        this.setNamespace(vo.getNamespace());
        this.setName(vo.getName());
        this.setEmergencyLevel(vo.getEmergencyLevel());
        this.setResourceId(vo.getResourceId());
        this.setError(vo.getError());
        this.setDataUuid(vo.getDataUuid());
        this.setAccountUuid(vo.getAccountUuid());
        this.setSubscriptionUuid(vo.getSubscriptionUuid());
        this.setReadStatus(vo.getReadStatus());
        this.setOperatorAccountUuid(vo.getOperatorAccountUuid());
        this.setLabels(vo.getLabels());
    }

    public static EventRecordsInventory valueOf(EventRecordsVO vo) {
        return new EventRecordsInventory(vo);
    }

    public static List<EventRecordsInventory> valueOf1(Collection<EventRecordsVO> vos) {
        List<EventRecordsInventory> invs = new ArrayList<EventRecordsInventory>(vos.size());
        for (EventRecordsVO vo : vos) {
            invs.add(EventRecordsInventory.valueOf(vo));
        }
        return invs;
    }

    public EventRecordsInventory() {
    }

    public long getId() {
        return id;
    }

    public void setId(long $paramName) {
        id = $paramName;
    }

    public long getCreateTime() {
        return createTime;
    }

    public void setCreateTime(long $paramName) {
        createTime = $paramName;
    }

    public String getNamespace() {
        return namespace;
    }

    public void setNamespace(String $paramName) {
        namespace = $paramName;
    }

    public String getName() {
        return name;
    }

    public void setName(String $paramName) {
        name = $paramName;
    }

    public String getEmergencyLevel() {
        return emergencyLevel;
    }

    public void setEmergencyLevel(String $paramName) {
        emergencyLevel = $paramName;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String $paramName) {
        resourceId = $paramName;
    }

    public String getError() {
        return error;
    }

    public void setError(String $paramName) {
        error = $paramName;
    }

    public String getDataUuid() {
        return dataUuid;
    }

    public void setDataUuid(String $paramName) {
        dataUuid = $paramName;
    }

    public String getAccountUuid() {
        return accountUuid;
    }

    public void setAccountUuid(String $paramName) {
        accountUuid = $paramName;
    }

    public String getSubscriptionUuid() {
        return subscriptionUuid;
    }

    public void setSubscriptionUuid(String $paramName) {
        subscriptionUuid = $paramName;
    }

    public Boolean getReadStatus() {
        return readStatus;
    }

    public void setReadStatus(Boolean $paramName) {
        readStatus = $paramName;
    }

    public String getOperatorAccountUuid() {
        return operatorAccountUuid;
    }

    public void setOperatorAccountUuid(String operatorAccountUuid) {
        this.operatorAccountUuid = operatorAccountUuid;
    }

    public String getLabels() {
        return labels;
    }

    public void setLabels(String $paramName) {
        labels = $paramName;
    }

    public static EventData toEventData(EventRecordsVO vo) {
        Gson gson = new Gson();
        EventData data = new EventData();
        data.setAccountUuid(vo.getAccountUuid());
        data.setDataUuid(vo.getDataUuid());
        data.setEmergencyLevel(vo.getEmergencyLevel());
        data.setName(vo.getName());
        data.setNamespace(vo.getNamespace());
        data.setReadStatus(vo.getReadStatus() ? "Read": "Unread");
        data.setResourceId(vo.getResourceId());
        data.setResourceName(vo.getResourceName());
        data.setSubscriptionUuid(vo.getSubscriptionUuid());
        data.setTime(vo.getCreateTime());
        data.setLabels(null);
        if (vo.getLabels() != null){
            Map<String, String> map = gson.fromJson(vo.getLabels(), new TypeToken<Map<String, String>>() {}.getType());
            data.setLabels(map);
        }
        return data;
    }
}
