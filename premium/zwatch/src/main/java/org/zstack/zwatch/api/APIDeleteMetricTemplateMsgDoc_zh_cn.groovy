package org.zstack.zwatch.api

import org.zstack.zwatch.api.APIDeleteMetricTemplateEvent

doc {
	title "DeleteMetricTemplate"

	category "zwatch"

	desc """在这里填写API描述"""

	rest {
		request {
			url "DELETE /v1/zwatch/metrics/httpreceivers/templates/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteMetricTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
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
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIDeleteMetricTemplateEvent.class
		}
	}
}