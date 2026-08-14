package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APIAttachAutoScalingTemplateToGroupEvent

doc {
	title "AttachAutoScalingTemplateToGroup"

	category "autoscaling"

	desc """挂载云主机模块到伸缩组"""

	rest {
		request {
			url "POST /v1/autoscaling/template/{uuid}/groups/{groupUuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAttachAutoScalingTemplateToGroupMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "模板UUID"
					location "url"
					type "String"
					optional false
					since "3.1.0"
				}
				column {
					name "groupUuid"
					enclosedIn "params"
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
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "3.1.0"
				}
			}
		}

		response {
			clz APIAttachAutoScalingTemplateToGroupEvent.class
		}
	}
}