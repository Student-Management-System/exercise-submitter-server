package net.ssehub.teaching.exercise_submitter.server.stu_mgmt;

import java.util.List;

import net.ssehub.studentmgmt.backend_api.model.NotificationDto;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;

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
    
    @Override
    public void sendSubmissionResult(SubmissionTarget target, List<ResultMessage> resultMessages) {
    }

}
