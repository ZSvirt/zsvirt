package org.zstack.ha;

import org.zstack.header.configuration.PythonClassInventory;
import org.zstack.header.search.Inventory;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @Author: DaoDao
 * @Date: 2023/4/4
 */
@Inventory(mappingVOClass = HaStrategyConditionVO.class)
@PythonClassInventory
public class HaStrategyConditionInventory implements Serializable {
    private String uuid;
    private String name;
    private String fencerName;
    private String state;
    private Timestamp createDate;
    private Timestamp lastOpDate;

    public HaStrategyConditionInventory() {
    }

    protected HaStrategyConditionInventory(HaStrategyConditionVO vo) {
        this.setUuid(vo.getUuid());
        this.setName(vo.getName());
        this.setFencerName(vo.getFencerName());
        this.setState(vo.getState().toString());
        this.setCreateDate(vo.getCreateDate());
        this.setLastOpDate(vo.getLastOpDate());
    }

    public static HaStrategyConditionInventory valueOf(HaStrategyConditionVO vo) {
        return new HaStrategyConditionInventory(vo);
    }

    public static List<HaStrategyConditionInventory> valueOf(Collection<HaStrategyConditionVO> vos) {
        List<HaStrategyConditionInventory> invs = new ArrayList<HaStrategyConditionInventory>(vos.size());
        for (HaStrategyConditionVO vo : vos) {
            invs.add(HaStrategyConditionInventory.valueOf(vo));
        }
        return invs;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFencerName() {
        return fencerName;
    }

    public void setFencerName(String fencerName) {
        this.fencerName = fencerName;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public Timestamp getCreateDate() {
        return createDate;
    }

    public void setCreateDate(Timestamp createDate) {
        this.createDate = createDate;
    }

    public Timestamp getLastOpDate() {
        return lastOpDate;
    }

    public void setLastOpDate(Timestamp lastOpDate) {
        this.lastOpDate = lastOpDate;
    }
}
