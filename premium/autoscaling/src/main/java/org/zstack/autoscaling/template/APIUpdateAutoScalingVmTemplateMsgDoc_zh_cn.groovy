package org.zstack.autoscaling.template

import org.zstack.autoscaling.template.APIUpdateAutoScalingTemplateEvent

doc {
	title "UpdateAutoScalingVmTemplate"

	category "autoscaling"

	desc """更新伸缩组云主机模板"""

	rest {
		request {
			url "PUT /v1/autoscaling/vmtemplate/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAutoScalingVmTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机模板UUID"
					location "url"
					type "String"
					optional false
					since "3.9.0"
				}
				column {
					name "name"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "模板名称"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "description"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "详细描述"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceName"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机名称"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceDescription"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机描述"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceOfferingUuid"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机实例规格UUID"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "imageUuid"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机镜像UUID"
					location "body"
					type "String"
					optional true
					since "3.9.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.9.0"
				}
				column {
					name "vmInstanceClusterUuid"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机所在集群UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
				column {
					name "hostUuid"
					enclosedIn "updateAutoScalingVmTemplate"
					desc "云主机所在物理机UUID"
					location "body"
					type "String"
					optional true
					since "3.10.0"
				}
			}
		}

		response {
			clz APIUpdateAutoScalingTemplateEvent.class
		}
	}
}