package org.zstack.autoscaling.group.rule



doc {

	title "伸缩组规则状态"

	field {
		name "Created"
		desc "已创建"
		type "AutoScalingRuleStatus"
		since "3.1.0"
	}
	field {
		name "WaitingForTrigger"
		desc "等待触发"
		type "AutoScalingRuleStatus"
		since "3.1.0"
	}
	field {
		name "Triggering"
		desc "已触发"
		type "AutoScalingRuleStatus"
		since "3.1.0"
	}
	field {
		name "Error"
		desc ""
		type "AutoScalingRuleStatus"
		since "3.1.0"
	}
}
