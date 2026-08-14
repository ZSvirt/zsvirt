package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APIAddVmToVmSchedulingRuleGroupEvent

doc {
	title "AddVmToVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """添加VM到VM调度组"""

	rest {
		request {
			url "POST /v1/vmSchedulingRuleGroup/{vmGroupUuid}/vmInstance/{vmUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddVmToVmSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "vmGroupUuid"
					enclosedIn ""
					desc "vm调度组uuid"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "vmUuid"
					enclosedIn ""
					desc "VM uuid"
					location "url"
					type "String"
					optional false
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
			clz APIAddVmToVmSchedulingRuleGroupEvent.class
		}
	}
}