package org.zstack.ovf.datatype;

/**
 * Created by Wenhao.Zhang on 22/03/09
 */
public enum OvfErrors {
    FAIL_TO_PARSE_OVF_XML(1001),
    INVALID_OVF_XML(1002),
    FAIL_TO_PARSE_IMAGE_INFO(1003),
    INVALID_IMAGE_INFO(1004),
    JOB_CANCELLED(1005),

    // OVF/VMDK upload issue
    OVF_UPLOAD_SUSPENDED(2001),
    OVF_UPLOAD_FAIL(2002),
    ;

    private String code;

    OvfErrors(int id) {
        code = String.format("OVF.%s", id);
    }

    @Override
    public String toString() {
        return code;
    }
}
