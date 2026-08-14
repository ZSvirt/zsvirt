package org.zstack.header.network.l3;

import java.util.*;

/**
 * Created by weiwang on 20/09/2017
 */
public enum L3NetworkCategory {
    Public("Public"),
    Private("Private"),
    System("System");

    String category;
    static public Map<Boolean, List<L3NetworkCategory>> validCombination = new HashMap<Boolean, List<L3NetworkCategory>>() {
        {
            List<L3NetworkCategory> notSystem = Arrays.asList(L3NetworkCategory.Public, L3NetworkCategory.Private);
            put(true, Arrays.asList(L3NetworkCategory.System));
            put(false, notSystem);
        }
    };


    L3NetworkCategory(String category) {
        this.category = category;
    }

    @Override
    public String toString() {
        return category;
    }

    public static Boolean checkSystemAndCategory(Boolean system, L3NetworkCategory category) {
        if (validCombination.get(system).contains(category)) {
            return true;
        } else {
            return false;
        }
    }
}
