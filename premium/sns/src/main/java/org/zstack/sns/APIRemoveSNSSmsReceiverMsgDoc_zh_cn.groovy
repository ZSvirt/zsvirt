package org.zstack.sns

import org.zstack.sns.APIRemoveSNSSmsReceiverEvent

doc {
	title "RemoveSNSSmsReceiver"

	category "sns"

	desc """删除短信接收者"""

	rest {
		request {
			url "DELETE /v1/sns/sms-endpoints/{endpointUuid}/receivers/{phoneNumber}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIRemoveSNSSmsReceiverMsg.class

			desc """"""

			params {

				column {
					name "endpointUuid"
					enclosedIn ""
					desc "短信接收端Uuid"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "phoneNumber"
					enclosedIn ""
					desc "短信接收号码"
					location "url"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "deleteMode"
					enclosedIn ""
					desc ""
					location "query"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIRemoveSNSSmsReceiverEvent.class
		}
	}
}