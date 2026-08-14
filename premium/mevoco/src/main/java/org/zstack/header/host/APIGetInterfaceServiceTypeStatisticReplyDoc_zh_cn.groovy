package org.zstack.header.host

import org.zstack.header.host.ServiceTypeStatisticData
import java.lang.Long
import org.zstack.header.errorcode.ErrorCode

doc {

	title "获取物理机网卡服务统计情况返回值"

	ref {
		name "serviceTypeStatistics"
		path "org.zstack.header.host.APIGetInterfaceServiceTypeStatisticReply.serviceTypeStatistics"
		desc "网卡网络服务情况统计结果列表"
		type "List"
		since "3.17.11"
		clz ServiceTypeStatisticData.class
	}
	field {
		name "total"
		desc "统计结果总数"
		type "Long"
		since "3.17.11"
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.11"
	}
	ref {
		name "error"
		path "org.zstack.header.host.APIGetInterfaceServiceTypeStatisticReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.11"
		clz ErrorCode.class
	}
}
