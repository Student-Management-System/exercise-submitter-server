package net.ssehub.teaching.exercise_submitter.server.submission;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import net.ssehub.teaching.exercise_submitter.server.rest.dto.CheckMessageDto;
import net.ssehub.teaching.exercise_submitter.server.rest.dto.SubmissionResultDto;
import net.ssehub.teaching.exercise_submitter.server.storage.ISubmissionStorage;
import net.ssehub.teaching.exercise_submitter.server.storage.NoSuchTargetException;
import net.ssehub.teaching.exercise_submitter.server.storage.StorageException;
import net.ssehub.teaching.exercise_submitter.server.storage.Submission;
import net.ssehub.teaching.exercise_submitter.server.storage.SubmissionTarget;
import net.ssehub.teaching.exercise_submitter.server.storage.Version;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Assignment;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.CheckConfiguration;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.Course;
import net.ssehub.teaching.exercise_submitter.server.stu_mgmt.StuMgmtView;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.Check;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.CheckstyleCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.CliJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.EncodingCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.FileUtils;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.InternalJavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.JavacCheck;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.ResultMessage.MessageType;
import net.ssehub.teaching.exercise_submitter.server.submission.checks.SrcFolderCheck;

/**
 * This class orchestrates a complete submission process. This should be the entry point for starting a submission.
 * 
 * @author Adam
 *
 */
public class SubmissionManager {
    
    private static final Logger LOGGER = Logger.getLogger(SubmissionManager.class.getName());
    
    private ISubmissionStorage storage;
    
    private StuMgmtView stuMgmtView;
    
    private List<Check> defaultRejectingChecks;
    
    /**
     * Creates a new {@link SubmissionManager}.
     * 
     * @param storage The storage component to use.
     * @param stuMgmtView The view on the student management system to inform about check results. 
     */
    public SubmissionManager(ISubmissionStorage storage, StuMgmtView stuMgmtView) {
        this.storage = storage;
        this.stuMgmtView = stuMgmtView;
        
        this.defaultRejectingChecks = new LinkedList<>();
    }
    
    /**
     * Adds a {@link Check} that will reject submissions if it fails. This is run for all submissions in all courses.
     * 
     * @param check The check to perform on submissions.
     */
    public void addDefaultRejectingCheck(Check check) {
        this.defaultRejectingChecks.add(check);
    }

    /**
     * Helper class to hold checks to run.
     */
    private static class Checks {
        private List<Check> rejecting = new LinkedList<>();
        private List<Check> nonRejecting = new LinkedList<>();
    }
    
    /**
     * Creates the {link Check}s to run for the given target.
     * 
     * @param target The target that is submitted to.
     * 
     * @return The {@link Check}s to run on the submission.
     */
    private Checks createChecks(SubmissionTarget target) {
        Checks result = new Checks();
        result.rejecting.addAll(defaultRejectingChecks);
        
        try {
            Course course = stuMgmtView.getCourse(target.getCourse()).orElseThrow();
            Assignment assignment = course.getAssignment(target.getAssignmentName()).orElseThrow();
            
            for (CheckConfiguration checkConfig : assignment.getCheckConfigurations()) {
                Check check = createCheck(checkConfig);
                if (checkConfig.isRejecting()) {
                    result.rejecting.add(check);
                } else {
                    result.nonRejecting.add(check);
                }
            }
            
        } catch (NoSuchElementException e) {
            LOGGER.warning(() -> "Could not get assignment " + target.getAssignmentName() + " in course "
                    + target.getCourse() + " to create checks; only running default rejecting checks");
        }
        
        return result;
    }
    
    /**
     * Creates a {@link Check} instance for the given {@link CheckConfiguration}.
     * <p>
     * Protected visibility for test cases.
     * 
     * @param checkConfiguration The configuration that specifies the check to create.
     * 
     * @return The created check.
     * 
     * @throws IllegalArgumentException If he check could not be created.
     */
    protected Check createCheck(CheckConfiguration checkConfiguration) throws IllegalArgumentException {
        Check result;
        
        switch (checkConfiguration.getCheckName()) {
        case EncodingCheck.CHECK_NAME:
            EncodingCheck encodingCheck = new EncodingCheck();
            checkConfiguration.getProperty("encoding")
                    .ifPresent(encoding -> encodingCheck.setWantedCharset(Charset.forName(encoding)));
            result = encodingCheck;
            break;
            
        case JavacCheck.CHECK_NAME:
            JavacCheck javacCheck = InternalJavacCheck.isSupported() ? new InternalJavacCheck() : new CliJavacCheck();
            checkConfiguration.getProperty("version")
                    .map(Integer::parseInt)
                    .ifPresent(version -> javacCheck.setJavaVersion(version));
            result = javacCheck;
            break;
            
        case CheckstyleCheck.CHECK_NAME:
            CheckstyleCheck checkstyleChek = new CheckstyleCheck(Path.of(checkConfiguration.getProperty("rules")
                    .orElseThrow(() -> new IllegalArgumentException("Checkstyle check must have \"rules\" set"))));
            result = checkstyleChek;
            break;
            
        case SrcFolderCheck.CHECK_NAME:
            result = new SrcFolderCheck();
            break;
        
        default:
            throw new IllegalArgumentException("Unknown check name: " + checkConfiguration.getCheckName());
        }
        
        return result;
    }
    
    /**
     * Executes a full submission.
     * <p>
     * This class runs the necessary {@link Check}s.
     * 
     * @param target The assignment and group to submit to.
     * @param submission The submission to add.
     * 
     * @return The result of the submission, including the messages created by the {@link Check}s.
     * 
     * @throws NoSuchTargetException If the given target does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    public SubmissionResultDto submit(SubmissionTarget target, Submission submission)
            throws NoSuchTargetException, StorageException {
        
        List<ResultMessage> checkMessages = new LinkedList<>();
        boolean accept;
        boolean hasAssignmentSpecificTests = false;
        
        if (submissionContentDiffers(target, submission)) {
            Checks checks = createChecks(target);
            hasAssignmentSpecificTests = checks.nonRejecting.size() > 0
                    || checks.rejecting.size() > defaultRejectingChecks.size();
            
            accept = runChecks(submission, checks, checkMessages);
            if (accept) {
                storage.submitNewVersion(target, submission);
            }
        } else {
            accept = false;
            checkMessages.add(new ResultMessage("submission", MessageType.WARNING,
                    "Submission is the same as the previous one"));
        }

        Collections.sort(checkMessages);
        
        LOGGER.info(() -> "Submission to " + target + " " + (accept ? "accepted" : "rejected")
                + "; messages: " + checkMessages);
        
        SubmissionResultDto result = new SubmissionResultDto();
        result.setAccepted(accept);
        result.setMessages(checkMessages.stream()
                .map(CheckMessageDto::new)
                .collect(Collectors.toList()));
        
        if (accept && hasAssignmentSpecificTests) {
            stuMgmtView.sendSubmissionResult(target, checkMessages);
        }
        
        return result;
    }
    
    /**
     * Checks if the given submission content differs from the latest submission in the storage.
     * 
     * @param target The target of the submission.
     * @param newSubmission The new submission.
     * 
     * @return Whether the new submission differs from the latest stored submission.
     * 
     * @throws NoSuchTargetException If the given target does not exist.
     * @throws StorageException If an exception occurred in the storage backend.
     */
    private boolean submissionContentDiffers(SubmissionTarget target, Submission newSubmission) 
            throws NoSuchTargetException, StorageException {

        boolean different;
        List<Version> versions = storage.getVersions(target);
        if (!versions.isEmpty()) {
            Submission latest = storage.getSubmission(target, versions.get(0));
            
            if (latest.getFilepaths().equals(newSubmission.getFilepaths())) {
                different = false;
                for (Path file : latest.getFilepaths()) {
                    if (!Arrays.equals(latest.getFileContent(file), newSubmission.getFileContent(file))) {
                        different = true;
                        break;
                    }
                }
                
            } else {
                different = true;
            }
            
        } else {
            different = true;
        }
        
        return different;
    }

    /**
     * Runs {@link Check}s on the given submission. For this, the submission is written to a temporary directory.
     * 
     * @param submission The submission to run {@link Check}s on.
     * @param checks The checks to run on the submission.
     * @param checkMessages {@link ResultMessage}s from the checks are added to this list.
     * 
     * @return Whether the submission should be accepted (<code>true</code>) or rejected (<code>false</code>).
     */
    private boolean runChecks(Submission submission, Checks checks, List<ResultMessage> checkMessages) {
        
        boolean accept = true;
        
        Path temporaryDirectory = null;
        try {
            temporaryDirectory = Files.createTempDirectory("exercise-submission");
            submission.writeToDirectory(temporaryDirectory);
            
            for (Check check : checks.rejecting) {
                boolean passed = check.run(temporaryDirectory);
                checkMessages.addAll(check.getResultMessages());
                
                if (!passed) {
                    accept = false;
                    break;
                }
            }
            
            if (accept) {
                for (Check check : checks.nonRejecting) {
                    check.run(temporaryDirectory);
                    checkMessages.addAll(check.getResultMessages());
                }
            }
            
            
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create temporary submission directory", e);
            accept = false;
            checkMessages.add(new ResultMessage("hook", MessageType.ERROR, "An internal error occurred"));
            
        } finally {
            if (temporaryDirectory != null) {
                try {
                    FileUtils.deleteDirectory(temporaryDirectory);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Failed to delete temporary submission directory", e);
                }
            }
        }
        
        return accept;
    }
    
}
