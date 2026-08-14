package org.zstack.header.vm

import org.zstack.header.vm.APIGetInterdependentL3NetworkImageReply

doc {
	title "获取相互依赖的镜像和三层网络(GetInterdependentL3NetworksImages)"

	category "vmInstance"

	desc """ZStack中一个三层网络属于一个区域，而镜像所在的镜像服务器可以加载到一个或多个区域。镜像服务器本身跟集群也存在依赖关系，例如CEPH的镜像服务器只能
跟CEPH的主存储一起工作。由于这种依赖关系的存在，创建云主机的时候指定的三层网络和镜像可能并不能一起工作。用户可以通过该API获得镜像或三层网络的相互依赖。

当指定了`l3NetworkUuids`参数时，返回的是可以跟这些三层网络一起工作的镜像的清单。
当指定了`imageUuid`参数时，返回的是可以跟该镜像一起工作的三层网络清单。
"""

	rest {
		request {
			url "GET /v1/images-l3networks/dependencies"

			header (Authorization: 'OAuth the-session-uuid')

			clz APIGetInterdependentL3NetworksImagesMsg.class

			desc """"""

			params {

				column {
					name "zoneUuid"
					enclosedIn ""
					desc "区域UUID。必须指定，以确定三层网络和镜像依赖关系。"
					location "query"
					type "String"
					optional false
					since "zsv 0.6"
				}
				column {
					name "l3NetworkUuids"
					enclosedIn ""
					desc "三层网络UUID列表"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "imageUuid"
					enclosedIn ""
					desc "镜像UUID"
					location "query"
					type "String"
					optional true
					since "zsv 0.6"
				}
				column {
					name "raiseException"
					enclosedIn ""
					desc "是否引发异常"
					location "query"
					type "boolean"
					optional true
					since "zsv 3.16.11"
				}
				column {
					name "systemTags"
					enclosedIn ""
					desc "系统标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
				column {
					name "userTags"
					enclosedIn ""
					desc "用户标签"
					location "query"
					type "List"
					optional true
					since "zsv 0.6"
				}
			}
		}

		response {
			clz APIGetInterdependentL3NetworkImageReply.class
		}
	}
}