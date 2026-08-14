package org.zstack.pciDevice

import org.zstack.pciDevice.APIUpdateHostIommuStateEvent

doc {
	title "UpdateHostIommuState"

	category "pciDevice"

	desc """在这里填写API描述"""

	rest {
		request {
			url "PUT /v1/pci-device/hosts/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUpdateHostIommuStateMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "updateHostIommuState"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
				}
				column {
					name "state"
					enclosedIn "updateHostIommuState"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("Enabled","Disabled")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc ""
					location "body"
					type "List"
					optional true
					since "0.6"
				}
			}
		}

		response {
			clz APIUpdateHostIommuStateEvent.class
		}
	}
}