package org.zstack.zwatch;

import org.zstack.header.core.ReturnValueCompletion;
import org.zstack.zwatch.api.GetMetricDataMsg;
import org.zstack.zwatch.api.GetMetricDataReply;
import org.zstack.zwatch.datatype.Datapoint;

import java.util.List;

public interface ZWatchManager {
    void getMetricData(GetMetricDataMsg msg, ReturnValueCompletion<List<Datapoint>> completion);
}
