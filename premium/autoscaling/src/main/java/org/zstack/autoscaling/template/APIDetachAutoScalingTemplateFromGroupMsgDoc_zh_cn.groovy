package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APIDetachAutoScalingTemplateFromGroupEvent

doc {
	title "DetachAutoScalingTemplateFromGroup"

	category "autoscaling"

	desc """卸载伸缩组模板"""

	rest {
		request {
			url "DELETE /v1/autoscaling/template/{templateUuid}/groups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDetachAutoScalingTemplateFromGroupMsg.class

			desc """"""

			params {

				column {
					name "templateUuid"
					enclosedIn ""
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "groupUuid"
					enclosedIn ""
					desc "伸缩组UUID"
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
			}
		}

		response {
			clz APIDetachAutoScalingTemplateFromGroupEvent.class
		}
	}
}