package org.zstack.header.core.external.service

import org.zstack.header.core.external.service.APIUpdateExternalServiceConfigurationEvent

doc {
	title "UpdateExternalServiceConfiguration"

	category "externalService"

	desc """更新外部服务配置"""

	rest {
		request {
			url "PUT /v1/external/service/configurations/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateExternalServiceConfigurationMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateExternalServiceConfiguration"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "description"
					enclosedIn "updateExternalServiceConfiguration"
					desc "资源的详细描述"
					location "body"
					type "String"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "configuration"
					enclosedIn "updateExternalServiceConfiguration"
					desc "外部服务配置内容。若 remote_write.basic_auth.password 使用查询结果中的脱敏值 ******，系统会保留该 remote_write 对应位置已保存的旧密码；如需修改密码，请提交真实密码"
					location "body"
					type "String"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "1.0.0, zsv_5.1.0"
				}
			}
		}

		response {
			clz APIUpdateExternalServiceConfigurationEvent.class
		}
	}
}