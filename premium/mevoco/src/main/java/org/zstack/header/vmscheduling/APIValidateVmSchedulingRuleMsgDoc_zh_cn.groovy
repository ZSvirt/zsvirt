package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIValidateVmSchedulingRuleReply

doc {
	title "ValidateVmSchedulingRule"

	category "vmSchedulingRule"

	desc """校验调度组策略"""

	rest {
		request {
			url "PUT /v1/validate/vmSchedulingRule"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIValidateVmSchedulingRuleMsg.class

			desc """"""

			params {

				column {
					name "vmGroupUuid"
					enclosedIn "validateVmSchedulingRule"
					desc "vm调度组uuid"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "hostGroupUuid"
					enclosedIn "validateVmSchedulingRule"
					desc "物理机调度组uuid"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "rule"
					enclosedIn "validateVmSchedulingRule"
					desc "调度规则"
					location "body"
					type "String"
					optional false
					since "3.16.0"
					values ("AFFINITY","ANTIAFFINITY")
				}
				column {
					name "mode"
					enclosedIn "validateVmSchedulingRule"
					desc "执行模式"
					location "body"
					type "String"
					optional false
					since "3.16.0"
					values ("SOFT","HARD")
				}
				column {
					name "zoneUuid"
					enclosedIn "validateVmSchedulingRule"
					desc "区域UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIValidateVmSchedulingRuleReply.class
		}
	}
}