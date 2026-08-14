package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APIDeleteAutoScalingTemplateEvent

doc {
	title "DeleteAutoScalingTemplate"

	category "autoscaling"

	desc """删除伸缩组模板"""

	rest {
		request {
			url "DELETE /v1/autoscaling/template/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteAutoScalingTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "query"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIDeleteAutoScalingTemplateEvent.class
		}
	}
}