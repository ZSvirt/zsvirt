package org.zstack.sns

import org.zstack.sns.APIAddSNSSmsReceiverEvent

doc {
	title "AddSNSSmsReceiver"

	category "sns"

	desc """添加短信接收者"""

	rest {
		request {
			url "POST /v1/sns/sms-endpoints/receivers"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSNSSmsReceiverMsg.class

			desc """"""

			params {

				column {
					name "phoneNumber"
					enclosedIn "params"
					desc "短信接收号码"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "短信接收端Uuid"
					location "body"
					type "String"
					optional false
					since "3.7.0"
				}
				column {
					name "type"
					enclosedIn "params"
					desc "短信接收端类型"
					location "body"
					type "String"
					optional false
					since "3.7.0"
					values ("AliyunSms")
				}
				column {
					name "description"
					enclosedIn "params"
					desc "短信接收者描述"
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "resourceUuid"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional true
					since "3.7.0"
				}
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "3.7.0"
				}
			}
		}

		response {
			clz APIAddSNSSmsReceiverEvent.class
		}
	}
}