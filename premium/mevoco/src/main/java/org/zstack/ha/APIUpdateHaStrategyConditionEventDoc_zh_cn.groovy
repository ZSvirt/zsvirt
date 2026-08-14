package org.zstack.ha

import org.zstack.ha.HaStrategyConditionInventory
import org.zstack.header.errorcode.ErrorCode

doc {

	title "更新 HA 策略的请求返回"

	ref {
		name "inventory"
		path "org.zstack.ha.APIUpdateHaStrategyConditionEvent.inventory"
		desc "HA 策略及条件清单"
		type "HaStrategyConditionInventory"
		since "3.17.0"
		clz HaStrategyConditionInventory.class
	}
	field {
		name "success"
		desc "请求是否成功"
		type "boolean"
		since "3.17.0"
	}
	ref {
		name "error"
		path "org.zstack.ha.APIUpdateHaStrategyConditionEvent.error"
		desc "错误码，若不为null，则表示操作失败, 操作成功时该字段为null"
		type "ErrorCode"
		since "3.17.0"
		clz ErrorCode.class
	}
}
