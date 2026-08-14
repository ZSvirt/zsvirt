package org.zstack.mttyDevice;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author yu.sun
 * @date 2022/11/28 13:59
 **/
public class MttyDeviceTO implements Serializable {
    private String uuid;

    private String name;

    private String description;

    private String hostUuid;

    private String type;


    private String virtStatus;

    public MttyDeviceTO() {
    }

    public MttyDeviceTO(MttyDeviceVO vo) {
        this.setUuid(vo.getUuid());
        this.setHostUuid(vo.getHostUuid());
    }

    public static MttyDeviceTO valueOf(MttyDeviceVO vo){
        return new MttyDeviceTO(vo);
    }

    public static List<MttyDeviceTO> valueOf(List<MttyDeviceVO> vos) {
        ArrayList<MttyDeviceTO> tos = new ArrayList<>();

        for (MttyDeviceVO vo : vos) {
            tos.add(new MttyDeviceTO(vo));
        }
        return tos;
    }
    @Override
    public String toString() {
        return "MttyDeviceTO{" +
                "uuid='" + uuid + '\'' +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", hostUuid='" + hostUuid + '\'' +
                ", type='" + type + '\'' +
                ", virtStatus='" + virtStatus + '\'' +
                '}';
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getHostUuid() {
        return hostUuid;
    }

    public void setHostUuid(String hostUuid) {
        this.hostUuid = hostUuid;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getVirtStatus() {
        return virtStatus;
    }

    public void setVirtStatus(String virtStatus) {
        this.virtStatus = virtStatus;
    }
}