package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import net.ssehub.studentmgmt.backend_api.model.NotificationDto;

public class EmptyStuMgmtView extends StuMgmtView {

    public EmptyStuMgmtView() {
        super(null, null, null, null);
    }
    
    @Override
    public void fullReload() throws StuMgmtLoadingException {
    }
    
    @Override
    public void update(NotificationDto notification) throws StuMgmtLoadingException {
    }

}
