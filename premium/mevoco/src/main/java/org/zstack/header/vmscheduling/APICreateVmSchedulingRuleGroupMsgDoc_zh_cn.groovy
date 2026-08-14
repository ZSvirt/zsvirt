package org.zstack.header.vmscheduling

import org.zstack.header.vmscheduling.APICreateVmSchedulingRuleGroupEvent

doc {
	title "CreateVmSchedulingRuleGroup"

	category "vmSchedulingRule"

	desc """创基vm调度组"""

	rest {
		request {
			url "POST /v1/vmSchedulingRuleGroup"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateVmSchedulingRuleGroupMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn "params"
					desc "区域UUID"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
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
			clz APICreateVmSchedulingRuleGroupEvent.class
		}
	}
}