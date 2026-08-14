package org.zstack.network.l2;

import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.zstack.core.db.DatabaseFacade;
import org.zstack.core.db.Q;
import org.zstack.header.network.l2.L2NetworkHostRefVO;
import org.zstack.header.network.l2.L2NetworkHostRefVO_;
import org.zstack.utils.CollectionUtils;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;


@Configurable(preConstruction = true, autowire = Autowire.BY_TYPE)
public class L2NetworkHostHelper {
    @Autowired
    DatabaseFacade dbf;

    private static final CLogger logger = Utils.getLogger(L2NetworkHostHelper.class);

    public void initL2NetworkHostRef(String l2NetworkUuid, List<String> hostUuids,
                                     String l2ProviderType, String bridgeName) {
        if (CollectionUtils.isEmpty(hostUuids)) {
            return;
        }

        List<L2NetworkHostRefVO> vos = new ArrayList<>();
        List<String> oldHosts = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.hostUuid)
                .eq(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuid)
                .in(L2NetworkHostRefVO_.hostUuid, hostUuids)
                .listValues();

        hostUuids.forEach(uuid -> {
            if (oldHosts.contains(uuid)) {
                return;
            }

            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(uuid);
            vo.setL2NetworkUuid(l2NetworkUuid);
            vo.setL2ProviderType(l2ProviderType);
            vo.setBridgeName(bridgeName);
            vos.add(vo);
            logger.debug(String.format("init %s", vo));
        });

        if (!vos.isEmpty()) {
            dbf.persistCollection(vos);
        }
    }

    public void initL2NetworkHostRef(String l2NetworkUuid, String hostUuid,
                                     String l2ProviderType, String bridgeName) {
        initL2NetworkHostRef(l2NetworkUuid, Collections.singletonList(hostUuid), l2ProviderType, bridgeName);
    }

    public void initL2NetworkHostRef(List<String> l2NetworkUuids, String hostUuid,
                                     String l2ProviderType, Map<String, String> bridgeNameMap) {
        if (CollectionUtils.isEmpty(l2NetworkUuids)) {
            return;
        }

        List<L2NetworkHostRefVO> newVos = new ArrayList<>();
        List<String> oldL2s = Q.New(L2NetworkHostRefVO.class)
                .select(L2NetworkHostRefVO_.l2NetworkUuid)
                .in(L2NetworkHostRefVO_.l2NetworkUuid, l2NetworkUuids)
                .eq(L2NetworkHostRefVO_.hostUuid, hostUuid)
                .listValues();

        l2NetworkUuids.forEach(l2Uuid -> {
            if (oldL2s.contains(l2Uuid)) {
                return;
            }

            L2NetworkHostRefVO vo = new L2NetworkHostRefVO();
            vo.setHostUuid(hostUuid);
            vo.setL2NetworkUuid(l2Uuid);
            vo.setL2ProviderType(l2ProviderType);
            vo.setBridgeName(bridgeNameMap.get(l2Uuid));
            newVos.add(vo);
            logger.debug(String.format("init %s", vo));
        });

        if (!newVos.isEmpty()) {
            dbf.persistCollection(newVos);
        }
    }
}
