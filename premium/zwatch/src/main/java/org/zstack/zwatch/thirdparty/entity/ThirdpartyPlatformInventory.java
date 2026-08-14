package org.zstack.zwatch.thirdparty.entity;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.message.DocUtils;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@PythonClassInventory
@Inventory(mappingVOClass = ThirdpartyPlatformVO.class, collectionValueOfMethod = "valueOf1")
public class ThirdpartyPlatformInventory implements Serializable {
    private String uuid;
    private String name;
    private String type;
    private String url;
    private String template;
    private String state;
    private String description;
    private Timestamp lastSyncDate;
    private Timestamp lastOpDate;
    private Timestamp createDate;

    protected ThirdpartyPlatformInventory(ThirdpartyPlatformVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setType(vo.getType());
        this.setUrl(vo.getUrl());
        this.setTemplate(vo.getTemplate());
        this.setState(vo.getState());
        this.setDescription(vo.getDescription());
        this.setLastSyncDate(vo.getLastSyncDate());
        this.setLastOpDate(vo.getLastOpDate());
        this.setCreateDate(vo.getCreateDate());
    }

    public static ThirdpartyPlatformInventory valueOf(ThirdpartyPlatformVO vo) {
        return new ThirdpartyPlatformInventory(vo);
    }

    public static List<ThirdpartyPlatformInventory> valueOf1(Collection<ThirdpartyPlatformVO> vos) {
        List<ThirdpartyPlatformInventory> invs = new ArrayList<ThirdpartyPlatformInventory>(vos.size());
        for (ThirdpartyPlatformVO vo : vos) {
            invs.add(ThirdpartyPlatformInventory.valueOf(vo));
        }
        return invs;
    }

    public ThirdpartyPlatformInventory() {
    }

    public static ThirdpartyPlatformInventory __example__() {
        ThirdpartyPlatformInventory inventory = new ThirdpartyPlatformInventory();
        inventory.setUuid("eec32b6fe11a464790641439f1d7c754");
        inventory.setName("xsky-172.20.11.24");
        String template = "{\n" +
                "    \"product\":\"XSKY\",\n" +
                "    \"service\":\"XSKY\",\n" +
                "    \"message\":\"${resource_type + '[' + resource_name+'] ' + group + ' ' + alert_value}\",\n" +
                "    \"metric\":\"${resource_type + '::' + group}\",\n" +
                "    \"alertLevel\":\"${level == 'info' ? 'Normal' : level == 'warning' ? 'Important' : 'Emergent'}\",\n" +
                "    \"alertTime\":\"${create}\",\n" +
                "    \"dimensions\":\"{'resource_name':'${resource_name}'}\",\n" +
                "    \"dataSource\":\"xsky-172.20.196.185\"\n" +
                "}";
        inventory.setTemplate(template);
        inventory.setUrl("http://172.20.11.24:8086/alerts/?token=001adb2ef25e41b7bd01b28651fcfa6a");
        inventory.setType("XSKY");
        inventory.setLastSyncDate(new Timestamp(DocUtils.date));
        inventory.setCreateDate(new Timestamp(DocUtils.date));
        inventory.setLastOpDate(new Timestamp(DocUtils.date));
        inventory.setDescription("desc");
        return inventory;
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

    public String getType() {
        return type;
    }

    public void setType(String $paramName) {
        type = $paramName;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String $paramName) {
        url = $paramName;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String $paramName) {
        template = $paramName;
    }

    public String getState() {
        return state;
    }

    public void setState(String $paramName) {
        state = $paramName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String $paramName) {
        description = $paramName;
    }

    public Timestamp getLastSyncDate() {
        return lastSyncDate;
    }

    public void setLastSyncDate(Timestamp $paramName) {
        lastSyncDate = $paramName;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp $paramName) {
        lastOpDate = $paramName;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp $paramName) {
        createDate = $paramName;
    }
}
