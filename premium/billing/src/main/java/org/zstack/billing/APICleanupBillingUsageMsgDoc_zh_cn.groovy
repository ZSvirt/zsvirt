package org.zstack.billing

import org.zstack.billing.APICleanupBillingUsageEvent

doc {
	title "CleanupBillingUsage"

	category "billing"

	desc """删除全部计费资源(例如：云主机，根云盘，数据盘)打点数据"""

	rest {
		request {
			url "DELETE /v1/billings/usage"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICleanupBillingUsageMsg.class

			desc """"""

			params {

				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APICleanupBillingUsageEvent.class
		}
	}
}