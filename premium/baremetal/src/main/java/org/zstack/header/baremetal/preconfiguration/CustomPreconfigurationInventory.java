package org.zstack.header.baremetal.preconfiguration;

import org.zstack.header.search.Inventory;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by GuoYi on 2018-12-28.
 */
@Inventory(mappingVOClass = CustomPreconfigurationVO.class)
public class CustomPreconfigurationInventory {
    private String uuid;
    private String baremetalInstanceUuid;
    private String param;
    private String value;

    public static CustomPreconfigurationInventory valueOf(CustomPreconfigurationVO vo) {
        CustomPreconfigurationInventory inv = new CustomPreconfigurationInventory();
        inv.setUuid(vo.getUuid());
        inv.setBaremetalInstanceUuid(vo.getBaremetalInstanceUuid());
        inv.setParam(vo.getParam());
        inv.setValue(vo.getValue());
        return inv;
    }

    public static List<CustomPreconfigurationInventory> valueOf(Collection<CustomPreconfigurationVO> vos) {
        return vos.stream().map(CustomPreconfigurationInventory::valueOf).collect(Collectors.toList());
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBaremetalInstanceUuid() {
        return baremetalInstanceUuid;
    }

    public void setBaremetalInstanceUuid(String baremetalInstanceUuid) {
        this.baremetalInstanceUuid = baremetalInstanceUuid;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
