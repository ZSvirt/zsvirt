package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APICreateResourceStackEvent

doc {
	title "CreateResourceStack"

	category "cloudformation"

	desc """创建资源编排堆栈"""

	rest {
		request {
			url "POST /v1/cloudformation/stack"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateResourceStackMsg.class

			desc """"""

			params {

				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.5.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "堆栈版本，默认为zstack"
					location "body"
					type "String"
					optional true
					since "2.5.0"
					values ("zstack")
				}
				column {
					name "rollback"
					enclosedIn "params"
					desc "堆栈创建失败是否回滚，默认回滚"
					location "body"
					type "Boolean"
					optional true
					since "2.5.0"
				}
				column {
					name "templateContent"
					enclosedIn "params"
					desc "堆栈内容，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "templateUuid"
					enclosedIn "params"
					desc "模板UUID"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "parameters"
					enclosedIn "params"
					desc "参数列表，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源的唯一UUID"
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.6.0"
				}
			}
		}

		response {
			clz APICreateResourceStackEvent.class
		}
	}
}