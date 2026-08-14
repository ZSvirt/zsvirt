package org.zstack.monitoring.targets;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.monitoring.items.Item;

import java.util.*;

/**
 * Created by xing5 on 2017/6/2.
 */

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public abstract class MonitorTarget {
    protected List<Item> items;

    public abstract String getTargetResourceUuid();

    public MonitorTarget(List<Item> items) {
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public Map<String, List<Item>> groupItemsByProvider() {
        Map<String, List<Item>> map = new HashMap<>();
        for (Item i : items) {
            List<Item> is = map.get(i.getProvider());
            if (is == null) {
                is = new ArrayList<>();
                map.put(i.getProvider(), is);
            }

            is.add(i);
        }

        return map;
    }

    public Collection<String> getProviderTypes() {
        return groupItemsByProvider().keySet();
    }
}
