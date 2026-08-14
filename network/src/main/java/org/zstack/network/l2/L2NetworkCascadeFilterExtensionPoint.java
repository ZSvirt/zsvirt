package org.zstack.network.l2;

import org.zstack.core.cascade.CascadeAction;
import org.zstack.header.network.l2.L2NetworkInventory;

import java.util.List;

/**
 * Created by boce on 14/07/2021
 */
public interface L2NetworkCascadeFilterExtensionPoint {
    List<L2NetworkInventory> filterL2NetworkCascade(List<L2NetworkInventory> l2invs, CascadeAction action);
}
