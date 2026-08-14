package org.zstack.cloudformation.template.decoder;

import org.zstack.core.Platform;
import org.zstack.header.errorcode.OperationFailureException;

/**
 * Created by mingjian.deng on 2020/3/4.
 */
public abstract class AbstractCfnRootDecoder implements CfnRootDecoder {
    protected int weight = 50;
    @Override
    public void setWeight(int weight) {
        if (weight > 100 || weight < 0) {
            throw new OperationFailureException(Platform.operr("CfnRootDecoder's weight must between 0-100, 0 means decode first, default is 50"));
        }
        this.weight = weight;
    }

    @Override
    public int getWeight() {
        return weight;
    }


}
