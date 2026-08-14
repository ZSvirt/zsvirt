package org.zstack.utils.filelocater;

import java.util.List;

public interface FileLocator {
    void addFolderFath(String path);
    
    void deleteFolderPath(String path);
    
    List<String> listAllFolderPaths();
    
    String getFilePath(String filename);
}
