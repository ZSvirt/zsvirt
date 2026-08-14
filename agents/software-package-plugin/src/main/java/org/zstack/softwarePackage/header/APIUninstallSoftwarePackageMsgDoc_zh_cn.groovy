package org.zstack.softwarePackage.header

doc {
	title "UninstallSoftwarePackage"

	category "softwarePackage"

	desc """卸载软件包"""

	rest {
		request {
			url "PUT /v1/software-package/{uuid}/actions"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIUninstallSoftwarePackageMsg.class

			desc """"""

			params {

				column {
					name "uuid"
					enclosedIn "uninstallSoftwarePackage"
					desc "软件包UUID"
					location "url"
					type "String"
					optional false
					since "4.10.20"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "4.10.20"
				}
			}
		}

		response {
			clz APIUninstallSoftwarePackageEvent.class
		}
	}
}