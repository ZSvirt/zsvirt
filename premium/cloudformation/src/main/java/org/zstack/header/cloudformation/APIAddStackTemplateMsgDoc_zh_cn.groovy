package org.zstack.header.cloudformation

import org.zstack.header.cloudformation.APIAddStackTemplateEvent

doc {
	title "AddStackTemplate"

	category "cloudformation"

	desc """添加资源编排模板"""

	rest {
		request {
			url "POST /v1/cloudformation/template"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddStackTemplateMsg.class

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
					desc "模板类型，默认为zstack"
					location "body"
					type "String"
					optional true
					since "2.5.0"
					values ("zstack")
				}
				column {
					name "templateContent"
					enclosedIn "params"
					desc "模板内容，json字符串"
					location "body"
					type "String"
					optional true
					since "2.5.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
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
			clz APIAddStackTemplateEvent.class
		}
	}
}