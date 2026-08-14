package org.zstack.compute.legacy;

import org.zstack.header.Component;

import static org.zstack.compute.legacy.ComputeLegacyGlobalProperty.*;

/**
 * Created by Wenhao.Zhang on 2024/10/25
 */
public class ComputeLegacyComponent implements Component {
    @Override
    public boolean start() {
        if (cpuTopologyFix) {
            new VmCpuTopologyFixHelper().run();
        }
        return true;
    }

    @Override
    public boolean stop() {
        return true;
    }
}
