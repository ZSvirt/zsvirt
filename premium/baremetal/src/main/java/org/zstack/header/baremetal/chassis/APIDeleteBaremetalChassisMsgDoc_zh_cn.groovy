package org.zstack.header.baremetal.chassis

import org.zstack.header.baremetal.chassis.APIDeleteBaremetalChassisEvent

doc {
	title "DeleteBaremetalChassis"

	category "baremetal.chassis"

	desc """删除裸机设备"""

	rest {
		request {
			url "DELETE /v1/baremetal/chassis/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIDeleteBaremetalChassisMsg.class

			desc """删除裸机设备"""

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
					name "deleteMode"
					enclosedIn ""
					desc "删除模式"
					location "query"
					type "String"
					optional true
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
			clz APIDeleteBaremetalChassisEvent.class
		}
	}
}