package org.zstack.mttyDevice;

import org.zstack.header.configuration.PythonClass;

/**
 * @author yu.sun
 * @date 2022/11/16 19:13
 **/
public class MttyDeviceConstants {
    @PythonClass
    public static final String SERVICE_ID = "mttyDevice";
    public static final String ACTION_CATEGORY = "mttyDevice";
    public static final String SE_Controller = "SE_Controller";
    public static final String VFIO_MDEV_VIRTUALIZABLE = "VFIO_MDEV_VIRTUALIZABLE";
    public static final String VFIO_MDEV_VIRTUALIZED = "VFIO_MDEV_VIRTUALIZED";
    public static final String UNVIRTUALIZABLE = "UNVIRTUALIZABLE";
    public static final String UNKNOWN = "UNKNOWN";


    public static final String SE_NUMBER = "se.num";
}