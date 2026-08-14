package org.zstack.accessKey

import org.zstack.accessKey.APICreateAccessKeyEvent

doc {
	title "CreateAccessKey"

	category "accessKey"

	desc """创建AccessKey"""

	rest {
		request {
			url "POST /v1/accesskeys"

			header (Authorization: 'OAuth the-session-uuid')

			clz APICreateAccessKeyMsg.class

			desc """"""

			params {

				column {
					name "accountUuid"
					enclosedIn "params"
					desc "账户UUID"
					location "body"
					type "String"
					optional false
					since "4.0.0"
				}
				column {
					name "userUuid"
					enclosedIn "params"
					desc "该值已弃用"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "description"
					enclosedIn "params"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "AccessKeyID"
					enclosedIn "params"
					desc "给定的AccessKeyID"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "AccessKeySecret"
					enclosedIn "params"
					desc "给定的AccessKeySecret"
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "4.0.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "4.0.0"
				}
			}
		}

		response {
			clz APICreateAccessKeyEvent.class
		}
	}
}