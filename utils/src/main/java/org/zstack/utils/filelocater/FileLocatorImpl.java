package org.zstack.utils.filelocater;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;


public class FileLocatorImpl implements FileLocator {
    private List<String> _folders = new ArrayList<String>();
    private static FileLocator _instance = null;
    
    @Override
    public void addFolderFath(String path) {
        _folders.add(path.trim());
    }

    @Override
    public void deleteFolderPath(String path) {
        _folders.remove(path);
    }

    @Override
    public List<String> listAllFolderPaths() {
        List<String> l = new ArrayList<String>();
        l.addAll(_folders);
        return l;
    }

    @Override
    public String getFilePath(String filename) {
        URL url = this.getClass().getClassLoader().getResource(filename);
        File f;
        if (url != null) {
            String fp = url.getPath();
            if (fp.contains(":")) {
                URL url2;
                try {
                    url2 = new URL(fp);
                } catch (MalformedURLException e) {
                    throw new RuntimeException("An error happended while searching file: " + filename, e);
                }
                fp = url2.getFile();
            }
            f = new File(fp);
            String fullp = f.getAbsolutePath();
            return fullp;
        }
        
        for (String folder : _folders) {
            String fullPath = folder + File.separator + filename;
            f = new File(fullPath);
            if (f.exists()) {
                return f.getAbsolutePath();
            }
        }
        
        return null;
    }
}
