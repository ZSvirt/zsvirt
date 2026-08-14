package org.zstack.storage.backup.imagestore;

import org.zstack.header.message.MessageReply;

import java.util.List;

public class ExportNbdImagesReply extends MessageReply {
    private List<String> imagePaths;
    private List<Integer> ports;
    private String nbdDescription;

    public List<String> getImagePaths() {
        return imagePaths;
    }

    public void setImagePaths(List<String> imagePaths) {
        this.imagePaths = imagePaths;
    }

    public List<Integer> getPorts() {
        return ports;
    }

    public void setPorts(List<Integer> ports) {
        this.ports = ports;
    }

    public String getNbdDescription() {
        return nbdDescription;
    }

    public void setNbdDescription(String nbdDescription) {
        this.nbdDescription = nbdDescription;
    }
}
