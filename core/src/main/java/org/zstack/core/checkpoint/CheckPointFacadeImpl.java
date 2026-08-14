package org.zstack.core.checkpoint;

public class CheckPointFacadeImpl implements CheckPointFacade {
    @Override
    public String execute(CheckPoint cp, String uuid) throws CloudCheckPointExecutionException {
        return execute(cp, uuid, true, null);
    }

    @Override
    public String execute(CheckPoint cp) throws CloudCheckPointExecutionException {
        return execute(cp, null);
    }

    @Override
    public String execute(CheckPoint cp, String uuid, boolean isReloadInput) throws CloudCheckPointExecutionException {
        return execute(cp, uuid, isReloadInput, null);
    }

    @Override
    public String execute(CheckPoint cp, String uuid, boolean isReloadInput, String[] bypassEntryNames)  throws CloudCheckPointExecutionException {
        CheckPointProxy cpp = new CheckPointProxy(cp, uuid, isReloadInput, bypassEntryNames);
        String chkUuid = null;
        try {
            cpp.execute();
            chkUuid = cpp.getCheckPointVO().getUuid();
        } catch (CloudCheckPointException e) { 
            /* This is internal error of advice, blow it up. Most cases are CheckPointContext has non-seriailizable members */
            throw e;
        } catch (Throwable t) {
            if (cpp.getCheckPointVO() != null) {
                /*
                 * Though it's rare, if any runtime exception happened in aspectj advice(e.g database has gone mad),
                 * cpp.getCheckPointVO().getUuid() will result in a NPE which overrides the original runtime exception. We avoid NPE here to 
                 * prevent that anti-pattern exception handling.
                 */
                chkUuid = cpp.getCheckPointVO().getUuid();
            }
            throw new CloudCheckPointExecutionException(chkUuid, t);
        }
        
        return chkUuid;
    }

    @Override
    public void cleanUp(CheckPoint cp, String uuid) {
        cleanUp(cp, uuid, true);
    }

    @Override
    public void cleanUp(CheckPoint cp, String uuid, boolean isReloadInput) {
       cleanUp(cp, uuid, isReloadInput, null); 
    }

    @Override
    public void cleanUp(CheckPoint cp, String uuid, boolean isReloadInput, String[] bypassEntryNames) {
        CheckPointProxy cpp = new CheckPointProxy(cp, uuid, isReloadInput, bypassEntryNames);
        cpp.cleanUp();
    }
}
