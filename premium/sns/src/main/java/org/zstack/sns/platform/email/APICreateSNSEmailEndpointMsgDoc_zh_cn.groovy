package org.zstack.sns.platform.email

import org.zstack.sns.APICreateSNSApplicationEndpointEvent

doc {
	title "CreateSNSEmailEndpoint"

	category "sns"

	desc """创建Email终端"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/emails"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateSNSEmailEndpointMsg.class

			desc """"""

			params {

				column {
					name "email"
					enclosedIn "params"
					desc "email地址"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "emails"
					enclosedIn "params"
					desc ""
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "name"
					enclosedIn "params"
					desc "资源名称"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "platformUuid"
					enclosedIn "params"
					desc "应用平台UUID"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APICreateSNSApplicationEndpointEvent.class
		}
	}
}