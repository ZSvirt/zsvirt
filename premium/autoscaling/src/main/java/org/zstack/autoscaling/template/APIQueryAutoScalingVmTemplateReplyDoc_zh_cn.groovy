package org.zstack.autoscaling.template

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.template.AutoScalingVmTemplateInventory

doc {

	title "查询伸缩组云主机模板返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.template.APIQueryAutoScalingVmTemplateReply.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventories"
		path "org.zstack.autoscaling.template.APIQueryAutoScalingVmTemplateReply.inventories"
		desc "null"
		type "List"
		since "3.1.0"
		clz AutoScalingVmTemplateInventory.class
	}
}
