package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APIRemoveSNSDingTalkAtPersonEvent

doc {
	title "RemoveSNSDingTalkAtPerson"

	category "sns"

	desc """删除钉钉@用户"""

	rest {
		request {
			url "DELETE /v1/sns/application-endpoints/ding-talk/{endpointUuid}/at-persons/{phoneNumber}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSNSDingTalkAtPersonMsg.class

			desc """"""

			params {

				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.3"
				}
				column {
					name "endpointUuid"
					enclosedIn ""
					desc "钉钉接收端UUID"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "phoneNumber"
					enclosedIn ""
					desc "要删除的atPerson的电话"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
			}
		}

		response {
			clz APIRemoveSNSDingTalkAtPersonEvent.class
		}
	}
}