package org.zstack.nas;

/**
 * Created by mingjian.deng on 2018/3/6.
 */
public interface NasFileSystemFactory {
    String getNasFileSystemType();

    NasFileSystem getNasFileSystem(NasFileSystemVO vo);

    NasFileSystem getNasMountTarget(NasMountTargetVO vo);

    NasFileSystem getNas();
}
