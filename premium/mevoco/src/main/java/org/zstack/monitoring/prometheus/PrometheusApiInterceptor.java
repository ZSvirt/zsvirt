package org.zstack.monitoring.prometheus;

import java.util.Arrays;
import java.util.List;
import org.zstack.header.apimediator.GlobalApiMessageInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.zstack.core.errorcode.ErrorFacade;
import org.zstack.header.apimediator.ApiMessageInterceptionException;
import org.zstack.header.message.APIMessage;
import org.zstack.utils.TimeUtils;
import static org.zstack.core.Platform.*;

import java.util.concurrent.TimeUnit;

/**
 * Created by xing5 on 2016/7/15.
 */
public class PrometheusApiInterceptor implements GlobalApiMessageInterceptor {
    @Autowired
    private ErrorFacade errf;

    @Override
    public APIMessage intercept(APIMessage msg) throws ApiMessageInterceptionException {
        if (msg instanceof APIPrometheusQueryMsg) {
            validate((APIPrometheusQueryMsg) msg);
        }

        return msg;
    }

    private void validate(APIPrometheusQueryMsg msg) {
        if (!msg.isInstant()) {
            if (msg.getRelativeTime() != null) {
                if (msg.getEndTime() == null) {
                    long now = System.currentTimeMillis();
                    msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(now));
                }

                try {
                    msg.setStartTime(msg.getEndTime() - TimeUnit.MILLISECONDS.toSeconds(TimeUtils.parseTimeInMillis(msg.getRelativeTime())));
                } catch (NumberFormatException e) {
                    throw new ApiMessageInterceptionException(argerr("the relativeTime[%s] is invalid, it must be in format of, for example, 10s, 1h", msg.getRelativeTime()));
                }

                if (msg.getStartTime() < 0) {
                    throw new ApiMessageInterceptionException(argerr("the relativeTime[%s] is invalid, it's too big", msg.getRelativeTime()));
                }
            } else {
                if (msg.getStartTime() == null) {
                    msg.setStartTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis() - TimeUnit.SECONDS.toMillis(10)));
                }
                if (msg.getEndTime() == null) {
                    msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
                }
            }

            if (msg.getStep() == null) {
                msg.setStep("10s");
            }
        } else {
            if (msg.getEndTime() == null) {
                msg.setEndTime(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
            }
        }
    }

    @Override
    @SuppressWarnings("rawtypes")
    public List<Class> getMessageClassToIntercept() {
        return Arrays.asList(
            APIPrometheusQueryLabelValuesMsg.class,
            APIPrometheusQueryMetadataMsg.class,
            APIPrometheusQueryPassThroughMsg.class,
            APIPrometheusQueryVmMonitoringDataMsg.class
        );
    }

    @Override
    public GlobalApiMessageInterceptor.InterceptorPosition getPosition() {
        return GlobalApiMessageInterceptor.InterceptorPosition.DEFAULT;
    }

}
