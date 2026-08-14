package org.zstack.autoscaling.group.rule

import java.lang.Long
import org.zstack.autoscaling.group.rule.AutoScalingRuleState
import org.zstack.autoscaling.group.rule.AutoScalingRuleStatus
import java.sql.Timestamp
import java.sql.Timestamp
import org.zstack.autoscaling.group.rule.trigger.AutoScalingRuleTriggerInventory

doc {

	title "伸缩组规则详细信息"

	field {
		name "type"
		desc "伸缩组规则类型"
		type "String"
		since "3.1.0"
	}
	field {
		name "description"
		desc "资源的详细描述"
		type "String"
		since "3.1.0"
	}
	field {
		name "cooldown"
		desc "冷却时间"
		type "Long"
		since "3.1.0"
	}
	ref {
		name "state"
		path "org.zstack.autoscaling.group.rule.AutoScalingRuleInventory.state"
		desc "伸缩组规则启用状态"
		type "AutoScalingRuleState"
		since "3.1.0"
		clz AutoScalingRuleState.class
	}
	ref {
		name "status"
		path "org.zstack.autoscaling.group.rule.AutoScalingRuleInventory.status"
		desc "伸缩组规则状态"
		type "AutoScalingRuleStatus"
		since "3.1.0"
		clz AutoScalingRuleStatus.class
	}
	field {
		name "systemTags"
		desc ""
		type "List"
		since "3.1.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "3.1.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "3.1.0"
	}
	field {
		name "name"
		desc "资源名称"
		type "String"
		since "3.1.0"
	}
	field {
		name "uuid"
		desc "资源的UUID，唯一标示该资源"
		type "String"
		since "3.1.0"
	}
	field {
		name "scalingGroupUuid"
		desc ""
		type "String"
		since "3.1.0"
	}
	ref {
		name "ruleTriggers"
		path "org.zstack.autoscaling.group.rule.AutoScalingRuleInventory.ruleTriggers"
		desc "null"
		type "List"
		since "3.1.0"
		clz AutoScalingRuleTriggerInventory.class
	}
}
