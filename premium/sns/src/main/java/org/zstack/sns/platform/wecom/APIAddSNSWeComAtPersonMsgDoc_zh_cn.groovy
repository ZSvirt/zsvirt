package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APIAddSNSWeComAtPersonEvent

doc {
	title "AddSNSWeComAtPerson"

	category "sns"

	desc """添加SNS企业微信@用户"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/we-com/at-persons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSNSWeComAtPersonMsg.class

			desc """"""

			params {

				column {
					name "userId"
					enclosedIn "params"
					desc "企业微信用户ID"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "企业微信终端UUID"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc "资源UUID"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "remark"
					enclosedIn "params"
					desc "备注"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIAddSNSWeComAtPersonEvent.class
		}
	}
}