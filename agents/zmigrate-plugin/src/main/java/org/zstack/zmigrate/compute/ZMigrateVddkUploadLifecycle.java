package org.zstack.zmigrate.compute;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class ZMigrateVddkUploadLifecycle {
    private static class UploadState {
        private boolean activationFinished;
        private boolean uploadSettled;
        private final List<Runnable> afterFinished = new ArrayList<>();
    }

    private final Map<String, UploadState> uploads = new HashMap<>();

    synchronized boolean begin(String uploadTaskUuid) {
        if (uploads.containsKey(uploadTaskUuid)) {
            return false;
        }
        uploads.put(uploadTaskUuid, new UploadState());
        return true;
    }

    synchronized boolean isCancelable(String uploadTaskUuid) {
        return !uploads.containsKey(uploadTaskUuid);
    }

    void runAfterFinished(String uploadTaskUuid, Runnable action) {
        synchronized (this) {
            UploadState state = uploads.get(uploadTaskUuid);
            if (state != null) {
                state.afterFinished.add(action);
                return;
            }
        }
        action.run();
    }

    void finishActivation(String uploadTaskUuid) {
        finish(uploadTaskUuid, true);
    }

    void markUploadSettled(String uploadTaskUuid) {
        finish(uploadTaskUuid, false);
    }

    private void finish(String uploadTaskUuid, boolean activationFinished) {
        List<Runnable> actions;
        synchronized (this) {
            UploadState state = uploads.get(uploadTaskUuid);
            if (state == null) {
                return;
            }
            if (activationFinished) {
                state.activationFinished = true;
            } else {
                state.uploadSettled = true;
            }
            if (!state.activationFinished || !state.uploadSettled) {
                return;
            }
            uploads.remove(uploadTaskUuid);
            actions = state.afterFinished;
        }
        actions.forEach(Runnable::run);
    }
}
