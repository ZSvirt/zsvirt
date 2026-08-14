package org.zstack.network.service.vip

import org.zstack.header.errorcode.ErrorCode

doc {

    title "操作返回结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.network.service.vip.APIDeleteVipEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "0.6"
        clz ErrorCode.class
    }
}
