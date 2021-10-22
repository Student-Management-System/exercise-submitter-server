package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import net.ssehub.studentmgmt.backend_api.ApiException;

public class EmptyStuMgmtView extends StuMgmtView {

    public EmptyStuMgmtView() throws ApiException {
        super(null);
    }
    
    @Override
    protected void init() throws ApiException {
    }

}
