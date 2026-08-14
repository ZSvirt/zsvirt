package org.zstack.cloudformation;

/**
 * Created by mingjian.deng on 2018/8/30.
 */
public enum  CloudFormationErrors {
    CREATE_FAILED(1000);

    private String code;

    CloudFormationErrors(int id) {
        code = String.format("CFM.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
