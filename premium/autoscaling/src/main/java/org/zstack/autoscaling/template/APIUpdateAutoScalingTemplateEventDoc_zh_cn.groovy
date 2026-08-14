package org.zstack.autoscaling.template

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.template.AutoScalingTemplateInventory

doc {

	title "更新伸缩组云主机模板返回"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.template.APIUpdateAutoScalingTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.9.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.autoscaling.template.APIUpdateAutoScalingTemplateEvent.inventory"
		desc "null"
		type "AutoScalingTemplateInventory"
		since "3.9.0"
		clz AutoScalingTemplateInventory.class
	}
}
