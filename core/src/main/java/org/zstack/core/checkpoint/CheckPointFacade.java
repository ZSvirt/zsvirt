package org.zstack.core.checkpoint;

public interface CheckPointFacade {
    String execute(CheckPoint cp) throws CloudCheckPointExecutionException;
    
    String execute(CheckPoint cp, String uuid) throws CloudCheckPointExecutionException;
    
    String execute(CheckPoint cp, String uuid, boolean isReloadInput) throws CloudCheckPointExecutionException;
    
    String execute(CheckPoint cp, String uuid, boolean isReloadInput, String[] bypassEntryNames) throws CloudCheckPointExecutionException;
    
    void cleanUp(CheckPoint cp, String uuid);
    
    void cleanUp(CheckPoint cp, String uuid, boolean isReloadInput);
    
    void cleanUp(CheckPoint cp, String uuid, boolean isReloadInput, String[] bypassEntryNames);
}
