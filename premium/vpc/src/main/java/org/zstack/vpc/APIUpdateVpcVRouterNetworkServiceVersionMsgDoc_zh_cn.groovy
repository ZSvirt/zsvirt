package org.zstack.vpc

doc {
    title "UpdateVirtualRouterSoftwareVersion"

    category "vpc"

    desc """在这里填写API描述"""

    rest {
        request {
			url "POST /v1/vpc/virtual-routers/{uuid}/softwareversion"

			header (Authorization: 'OAuth the-session-uuid')

            clz APIUpdateVirtualRouterSoftwareVersionMsg.class

            desc """"""
            
			params {

				column {
					name "uuid"
					enclosedIn "params"
					desc "资源的UUID，唯一标示该资源"
					location "url"
					type "String"
					optional false
					since "0.6"
					
				}
				column {
					name "networkService"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("IPsec")
				}
				column {
					name "targetVersion"
					enclosedIn "params"
					desc ""
					location "body"
					type "String"
					optional false
					since "0.6"
					values ("4.5.2","5.9.4")
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "body"
					type "List"
					optional true
					since "0.6"
					
				}
			}
        }

        response {
            clz APIUpdateVirtualRouterSoftwareVersionEvent.class
        }
    }
}