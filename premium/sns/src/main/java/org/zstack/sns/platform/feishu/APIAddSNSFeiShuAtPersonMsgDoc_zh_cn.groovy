package org.zstack.sns.platform.feishu

import org.zstack.sns.platform.feishu.APIAddSNSFeiShuAtPersonEvent

doc {
	title "AddSNSFeiShuAtPerson"

	category "sns"

	desc """添加SNS飞书@用户"""

	rest {
		request {
			url "POST /v1/sns/application-endpoints/feishu/at-persons"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIAddSNSFeiShuAtPersonMsg.class

			desc """"""

			params {

				column {
					name "userId"
					enclosedIn "params"
					desc "飞书用户ID"
					location "body"
					type "String"
					optional false
					since "zsv 4.2.0"
				}
				column {
					name "endpointUuid"
					enclosedIn "params"
					desc "飞书终端UUID"
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
			clz APIAddSNSFeiShuAtPersonEvent.class
		}
	}
}