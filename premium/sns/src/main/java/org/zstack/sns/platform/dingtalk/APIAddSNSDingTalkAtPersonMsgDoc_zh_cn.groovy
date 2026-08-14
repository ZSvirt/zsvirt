package org.zstack.sns.platform.dingtalk

import org.zstack.sns.platform.dingtalk.APIAddSNSDingTalkAtPersonEvent

doc {
	title "AddSNSDingTalkAtPerson"

	category "sns"

	desc """添加钉钉@用户"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/ding-talk/at-persons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSNSDingTalkAtPersonMsg.class

			desc """"""

			params {

				column {
					name "phoneNumber"
					enclosedIn "params"
					desc "用户电话号码（钉钉用户以电话注册）"
					location "body"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "钉钉终端UUID"
					location "body"
					type "String"
					optional false
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
				column {
					name "tagUuids"
					enclosedIn "params"
					desc "标签UUID列表"
					location "body"
					type "List"
					optional true
					since "3.4.0"
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
			clz APIAddSNSDingTalkAtPersonEvent.class
		}
	}
}