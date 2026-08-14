package org.zstack.zwatch.monitorgroup.api

import org.zstack.zwatch.monitorgroup.api.APIDeleteMetricRuleTemplateEvent

doc {
	title "DeleteMetricRuleTemplate"

	category "zwatch"

	desc """删除资源报警模板"""

	rest {
		request {
			url "DELETE /v1/zwatch/monitortemplates/metricrules/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMetricRuleTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源报警模板UUID"
					location "url"
					type "String"
					optional false
					since "3.10.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIDeleteMetricRuleTemplateEvent.class
		}
	}
}