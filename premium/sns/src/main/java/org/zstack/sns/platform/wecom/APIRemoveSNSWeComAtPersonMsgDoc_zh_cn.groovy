package org.zstack.sns.platform.wecom

import org.zstack.sns.platform.wecom.APIRemoveSNSWeComAtPersonEvent

doc {
	title "RemoveSNSWeComAtPerson"

	category "sns"

	desc """删除SNS企业微信@用户"""

	rest {
		request {
			url "DELETE /v1/sns/application-endpoints/we-com/{endpointUuid}/at-persons/{userId}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSNSWeComAtPersonMsg.class

			desc """"""

			params {

				column {
					name "endpointUuid"
					enclosedIn ""
					desc "企业微信终端UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "userId"
					enclosedIn ""
					desc "企业微信用户ID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc "删除模式(Permissive / Enforcing，Permissive)"
					location "query"
					type "String"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 4.2.0"
				}
			}
		}

		response {
			clz APIRemoveSNSWeComAtPersonEvent.class
		}
	}
}