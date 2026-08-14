package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APIUpdateAtPersonOfWeComEndpointEvent

doc {
	title "UpdateAtPersonOfAtWeComEndpoint"

	category "sns"

	desc """更新企业微信@用户"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/we-com/at-persons/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAtPersonOfAtWeComEndpointMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAtPersonOfAtWeComEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "updateAtPersonOfAtWeComEndpoint"
					desc "企业微信终端UUID"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "userId"
					enclosedIn "updateAtPersonOfAtWeComEndpoint"
					desc "企业微信用户ID"
					location "body"
					type "String"
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
					enclosedIn "updateAtPersonOfAtWeComEndpoint"
					desc "备注"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIUpdateAtPersonOfWeComEndpointEvent.class
		}
	}
}