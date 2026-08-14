package org.zstack.iam1.header;

/**
 * Created by Wenhao.Zhang on 2024/08/30
 */
public enum IAM1Errors {
    GENERAL_ERROR(1000),

    GROUP_MOVE_TO_WRONG_PLACE(2001),

    NOT_RESOURCE_ENSEMBLE_MEMBER(3001),
    ;

    private String code;

    private IAM1Errors(int id) {
        code = String.format("IAM1.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
