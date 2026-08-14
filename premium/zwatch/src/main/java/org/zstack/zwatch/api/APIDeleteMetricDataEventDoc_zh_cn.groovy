package org.zstack.zwatch.api

import org.zstack.header.errorcode.ErrorCode

doc {

    title "删除监控数据"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
    ref {
        name "error"
        path "org.zstack.zwatch.api.APIDeleteMetricDataEvent.error"
        desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null", false
        type "ErrorCode"
        since "3.2.0"
        clz ErrorCode.class
    }
}
