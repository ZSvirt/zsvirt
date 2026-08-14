package org.zstack.header.cluster

import org.zstack.header.errorcode.ErrorCode
import org.zstack.header.longjob.LongJobInventory

doc {

	title "集群内物理机操作系统升级结果"

	field {
		name "success"
		desc ""
		type "boolean"
		since "zsv 0.6"
	}
	ref {
		name "error"
		path "org.zstack.header.cluster.APIUpdateClusterOSEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "zsv 2.3"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.header.cluster.APIUpdateClusterOSEvent.inventory"
		desc "任务信息"
		type "LongJobInventory"
		since "zsv 2.2"
		clz LongJobInventory.class
	}
}
