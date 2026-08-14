package org.zstack.autoscaling.template

import org.zstack.header.errorcode.ErrorCode
import org.zstack.autoscaling.template.AutoScalingTemplateInventory

doc {

	title "创建云主机模板返回值"

	field {
		name "success"
		desc ""
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.autoscaling.template.APICreateAutoScalingTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null",false
		type "ErrorCode"
		since "3.1.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.autoscaling.template.APICreateAutoScalingTemplateEvent.inventory"
		desc "null"
		type "AutoScalingTemplateInventory"
		since "3.1.0"
		clz AutoScalingTemplateInventory.class
	}
}
