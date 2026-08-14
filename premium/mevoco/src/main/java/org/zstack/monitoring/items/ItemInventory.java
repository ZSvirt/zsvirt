package org.zstack.monitoring.items;

/**
 * Created by xing5 on 2017/6/28.
 */
public class ItemInventory {
    private String name;
    private String readableName;

    public ItemInventory() {
    }

    public ItemInventory(Item item) {
        name = item.getName();
        readableName = item.getReadableName();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getReadableName() {
        return readableName;
    }

    public void setReadableName(String readableName) {
        this.readableName = readableName;
    }
}
