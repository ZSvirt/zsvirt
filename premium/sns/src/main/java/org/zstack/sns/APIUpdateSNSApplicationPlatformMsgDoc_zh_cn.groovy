package org.zstack.sns

import org.zstack.sns.APIUpdateSNSApplicationPlatformEvent

doc {
	title "UpdateSNSApplicationPlatform"

	category "sns"

	desc """更新SNS应用平台"""

	rest {
		request {
			url "PUT /v1/sns/application-platforms/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateSNSApplicationPlatformMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateSNSApplicationPlatform"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.3"
				}
				column {
					name "name"
					enclosedIn "updateSNSApplicationPlatform"
					desc "资源名称"
					location "body"
					type "String"
					optional true
					since "2.3"
				}
				column {
					name "description"
					enclosedIn "updateSNSApplicationPlatform"
					desc "资源的详细描述"
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
					desc "应用标签"
					location "body"
					type "List"
					optional true
					since "2.3"
				}
			}
		}

		response {
			clz APIUpdateSNSApplicationPlatformEvent.class
		}
	}
}