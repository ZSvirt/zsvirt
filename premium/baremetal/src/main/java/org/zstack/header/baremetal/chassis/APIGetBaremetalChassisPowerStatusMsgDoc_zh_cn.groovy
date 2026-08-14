package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIGetBaremetalChassisPowerStatusReply

doc {
	title "GetBaremetalChassisPowerStatus"

	category "baremetal.chassis"

	desc """获取裸机设备电源状态"""

	rest {
		request {
			url "GET /v1/baremetal/chassis/{uuid}/powerstatus"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetBaremetalChassisPowerStatusMsg.class

			desc """获取裸机设备电源状态"""

			params {

				column {
					name "uuid"
					enclosedIn ""
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "2.6.0"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "2.6.0"
				}
			}
		}

		response {
			clz APIGetBaremetalChassisPowerStatusReply.class
		}
	}
}