package org.zstack.compute.legacy;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.Q;
import org.zstack.core.db.SQL;
import org.zstack.header.vm.VmInstanceVO;
import org.zstack.resourceconfig.ResourceConfigFacade;
import org.zstack.tag.SystemTagCreator;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import javax.persistence.Tuple;
import java.util.List;
import java.util.Map;

import static org.zstack.compute.vm.VmHardwareSystemTags.*;
import static org.zstack.utils.CollectionDSL.*;
import static org.zstack.utils.CollectionUtils.*;

@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class VmCpuTopologyFixHelper {
    private static final CLogger logger = Utils.getLogger(VmCpuTopologyFixHelper.class);

    @Autowired
    private ResourceConfigFacade resourceConfigFacade;

    /**
     * in issue ZSV-7145
     *
     * Before ZSphere version 4.10.0, MN recorded the cpuSocket of VM, not cpuCore;
     * After upgrading to version 4.10.0, MN no longer defaults to setting VM's cpuSocket,
     * but instead records cpuCore;
     *
     * The current requirement is to transition from cpuSocket data
     * to cpuCore without affecting the previous CPU topology after the upgrade.
     */
    public void run() {
        String sql = "select uuid,cpuNum from VmInstanceVO";
        long total = Q.New(VmInstanceVO.class).count();

        SQL.New(sql, Tuple.class)
                .limit(1000)
                .paginate(total, (List<Tuple> tuples) -> {
                    Map<String, Integer> vmCpuMap = toMap(tuples,
                            tuple -> tuple.get(0, String.class),
                            tuple -> tuple.get(1, Integer.class));
                    handleVmList(vmCpuMap);
                });
    }

    @SuppressWarnings("unchecked")
    private void handleVmList(Map<String, Integer> vmCpuMap) {
        Map<String, List<String>> socketsTags = CPU_SOCKETS.getTags(vmCpuMap.keySet());
        Map<String, List<String>> coreTags = CPU_CORES.getTags(vmCpuMap.keySet());

        for (String vmUuid : vmCpuMap.keySet()) {
            if (coreTags.containsKey(vmUuid) || !socketsTags.containsKey(vmUuid)) {
                continue;
            }

            String socketTag = socketsTags.get(vmUuid).get(0);
            int socketNum = Integer.parseInt(CPU_SOCKETS.getTokenByTag(socketTag, CPU_SOCKETS_TOKEN));
            int cpuNum = vmCpuMap.get(vmUuid);
            int coresPerSocket = (int) Math.ceil((double) cpuNum / socketNum);

            final SystemTagCreator creator = CPU_CORES.newSystemTagCreator(vmUuid);
            creator.recreate = false;
            creator.setTagByTokens(map(e(CPU_CORES_TOKEN, coresPerSocket)));
            creator.create();

            CPU_SOCKETS.delete(vmUuid);

            logger.info(String.format("Fix VM[uuid:%s] CPU topology from socket[%d] to core[%d]",
                    vmUuid, socketNum, coresPerSocket));
        }
    }
}
