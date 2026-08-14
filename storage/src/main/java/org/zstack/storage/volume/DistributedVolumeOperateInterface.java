package org.zstack.storage.volume;

/**
 * Created by mingjian.deng on 16/11/9.
 */
public interface DistributedVolumeOperateInterface {

    public String exportFile(String srcPath, String desPath);
    public void importFile(String srcPath, String desPath);
}
