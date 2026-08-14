package org.zstack.core.checkpoint;

public interface CheckPoint {
    void execute();
    
    void cleanUp();
}
