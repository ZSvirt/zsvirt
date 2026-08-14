package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APIRemoveSNSFeiShuAtPersonEvent

doc {
	title "RemoveSNSFeiShuAtPerson"

	category "sns"

	desc """删除SNS飞书@用户"""

	rest {
		request {
			url "DELETE /v1/sns/application-endpoints/feishu/{endpointUuid}/at-persons/{userId}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSNSFeiShuAtPersonMsg.class

			desc """"""

			params {

				column {
					name "endpointUuid"
					enclosedIn ""
					desc "飞书终端UUID"
					location "url"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "userId"
					enclosedIn ""
					desc "飞书用户ID"
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
			clz APIRemoveSNSFeiShuAtPersonEvent.class
		}
	}
}