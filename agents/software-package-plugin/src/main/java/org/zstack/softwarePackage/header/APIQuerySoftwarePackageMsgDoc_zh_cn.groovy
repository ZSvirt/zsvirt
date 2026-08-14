package org.zstack.softwarePackage.header


import org.zstack.header.query.APIQueryMessage

doc {
	title "QuerySoftwarePackage"

	category "softwarePackage"

	desc """查询软件包"""

	rest {
		request {
			url "GET /v1/software-package"
			url "GET /v1/software-package/{uuid}"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIQuerySoftwarePackageMsg.class

			desc """"""

			params APIQueryMessage.class
		}

		response {
			clz APIQuerySoftwarePackageReply.class
		}
	}
}