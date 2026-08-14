package org.zstack.header.vmscheduling

import org.zstack.header.affinitygroup.APIDeleteAffinityGroupEvent

doc {
	title "RemoveVmSchedulingRule"

	category "vmSchedulingRule"

	desc """移除vm调度策略"""

	rest {
		request {
			url "DELETE /v1/vmSchedulingRule/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveVmSchedulingRuleMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.16.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.16.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.16.0"
				}
			}
		}

		response {
			clz APIDeleteAffinityGroupEvent.class
		}
	}
}