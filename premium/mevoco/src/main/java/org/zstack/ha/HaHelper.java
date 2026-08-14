package org.zstack.ha;

import org.apache.commons.collections.CollectionUtils;
import org.zstack.core.db.Q;
import org.zstack.utils.Utils;
import org.zstack.utils.logging.CLogger;

import java.util.List;

/**
 * @Author: ya.wang
 * @Date: 12/20/24 11:39
 */
public class HaHelper {
    private static final CLogger logger = Utils.getLogger(HaHelper.class);

    public static List<String> findExecuteFencers() {
        List<String> fencers = Q.New(HaStrategyConditionVO.class)
                .select(HaStrategyConditionVO_.fencerName)
                .eq(HaStrategyConditionVO_.state, HaStrategyState.Enable)
                .listValues();

        if (CollectionUtils.isEmpty(fencers) || !fencers.contains(HaConstants.HOST_STORAGE_STATE)) {
            fencers.add(HaConstants.HOST_STORAGE_STATE);
        }

        return fencers;
    }

}
