package org.zstack.storage.primary.block.vendor.xstor;

/**
 * @author Lei Liu lei.liu@zstack.io
 * @date 2022/4/13 10:58
 */
public class XStorServerResponse {
    public String detail_err_msg;
    public String err_msg;
    public Integer err_no;

    public Integer getErr_no() {
        return err_no;
    }

    public String getDetail_err_msg() {
        return detail_err_msg;
    }

    public String getErr_msg() {
        return err_msg;
    }
}
