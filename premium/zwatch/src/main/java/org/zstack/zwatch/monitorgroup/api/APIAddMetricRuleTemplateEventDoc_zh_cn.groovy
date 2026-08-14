package org.zstack.zwatch.monitorgroup.api

import org.zstack.header.errorcode.ErrorCode
import org.zstack.zwatch.monitorgroup.entity.MetricRuleTemplateInventory

doc {

	title "添加资源报警模板返回"

	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "0.6"
	}
	ref {
		name "error"
		path "org.zstack.zwatch.monitorgroup.api.APIAddMetricRuleTemplateEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.10.0"
		clz ErrorCode.class
	}
	ref {
		name "inventory"
		path "org.zstack.zwatch.monitorgroup.api.APIAddMetricRuleTemplateEvent.inventory"
		desc "资源报警模板清单"
		type "MetricRuleTemplateInventory"
		since "3.10.0"
		clz MetricRuleTemplateInventory.class
	}
}
