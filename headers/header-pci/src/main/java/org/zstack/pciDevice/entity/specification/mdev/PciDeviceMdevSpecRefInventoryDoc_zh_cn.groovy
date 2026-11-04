package org.zstack.pciDevice.entity.specification.mdev

import java.lang.Boolean
import java.sql.Timestamp
import java.sql.Timestamp

doc {

	title "PCI设备可用MDEV规格"

	field {
		name "pciDeviceUuid"
		desc "PCI设备UUID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "mdevSpecUuid"
		desc "MDEV设备规格UUID"
		type "String"
		since "zsv_3.5.0"
	}
	field {
		name "effective"
		desc "当前MDEV规格是否被用于切分该PCI设备"
		type "Boolean"
		since "zsv_3.5.0"
	}
	field {
		name "createDate"
		desc "创建时间"
		type "Timestamp"
		since "zsv_3.5.0"
	}
	field {
		name "lastOpDate"
		desc "最后一次修改时间"
		type "Timestamp"
		since "zsv_3.5.0"
	}
}
