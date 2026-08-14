package org.zstack.core.checkpoint;


public class CheckPointProxy implements CheckPoint {
    private final CheckPoint cp;
    private final boolean isReloadInput;
    private final String[] bypassEntryNames;
    private final String chkUuid;
    private CheckPointVO checkPointVO;

    CheckPointProxy(CheckPoint cp, String chkUuid, boolean isReloadInput, String[] bypassEntryNames) {
       this.cp = cp; 
       this.isReloadInput = isReloadInput;
       this.bypassEntryNames = bypassEntryNames;
       this.chkUuid = chkUuid;
    }
    
    String getCheckPointUuid() {
        return chkUuid;
    }
    
    String[] getBypassCheckPointEntires() {
        return bypassEntryNames;
    }

    @Override
    public void execute() {
        cp.execute();
    }

    boolean isReloadInput() {
        return this.isReloadInput;
    }

    CheckPointVO getCheckPointVO() {
        return checkPointVO;
    }

    void setCheckPointVO(CheckPointVO checkPointVO) {
        this.checkPointVO = checkPointVO;
    }

    CheckPoint getCheckPoint() {
        return cp;
    }

    @Override
    public void cleanUp() {
        cp.cleanUp(); 
    }
}
