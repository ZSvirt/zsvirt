package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIUpdateResourceStackEvent

doc {
	title "UpdateResourceStack"

	category "cloudformation"

	desc """修改资源编排堆栈"""

	rest {
		request {
			url "PUT /v1/cloudformation/stack/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateResourceStackMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateResourceStack"
					desc "堆栈的UUID"
					location "url"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "name"
					enclosedIn "updateResourceStack"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "description"
					enclosedIn "updateResourceStack"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "rollback"
					enclosedIn "updateResourceStack"
					desc "创建失败是否回滚"
					location "body"
					type "Boolean"
					optional true
					since "2.5.0"
				}
				column {
					name "templateContent"
					enclosedIn "updateResourceStack"
					desc "堆栈内容，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "parameters"
					enclosedIn "updateResourceStack"
					desc "堆栈参数列表，json字符串"
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
			clz APIUpdateResourceStackEvent.class
		}
	}
}