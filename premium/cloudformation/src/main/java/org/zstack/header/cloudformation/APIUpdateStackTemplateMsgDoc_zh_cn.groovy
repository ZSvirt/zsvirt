package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIUpdateStackTemplateEvent

doc {
	title "UpdateStackTemplate"

	category "cloudformation"

	desc """修改资源编排模板"""

	rest {
		request {
			url "PUT /v1/cloudformation/template/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateStackTemplateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateStackTemplate"
					desc "模板的UUID"
					location "url"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "name"
					enclosedIn "updateStackTemplate"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "description"
					enclosedIn "updateStackTemplate"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "state"
					enclosedIn "updateStackTemplate"
					desc "模板是否可用"
					location "body"
					type "Boolean"
					optional true
					since "2.5.0"
				}
				column {
					name "templateContent"
					enclosedIn "updateStackTemplate"
					desc "模板内容，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.5.0"
				}
			}
		}

		response {
			clz APIUpdateStackTemplateEvent.class
		}
	}
}