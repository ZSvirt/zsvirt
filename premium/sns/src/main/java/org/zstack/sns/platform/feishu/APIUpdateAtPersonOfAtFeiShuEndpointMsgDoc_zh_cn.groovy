package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APIUpdateAtPersonOfFeiShuEndpointEvent

doc {
	title "UpdateAtPersonOfAtFeiShuEndpoint"

	category "sns"

	desc """更新飞书@用户"""

	rest {
		request {
			url "PUT /v1/sns/application-endpoints/feishu/at-persons/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateAtPersonOfAtFeiShuEndpointMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateAtPersonOfAtFeiShuEndpoint"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "updateAtPersonOfAtFeiShuEndpoint"
					desc "飞书终端UUID"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "userId"
					enclosedIn "updateAtPersonOfAtFeiShuEndpoint"
					desc "飞书用户ID"
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
					enclosedIn "updateAtPersonOfAtFeiShuEndpoint"
					desc "备注"
					location "body"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIUpdateAtPersonOfFeiShuEndpointEvent.class
		}
	}
}