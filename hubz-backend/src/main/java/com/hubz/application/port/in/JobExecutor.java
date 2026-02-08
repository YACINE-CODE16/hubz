package com.hubz.application.port.in;

import com.hubz.domain.enums.JobType;

/**
 * Interface for executing background jobs.
 * Each implementation handles a specific job type.
 */
public interface JobExecutor {

    /**
     * Execute the job with the given payload.
     *
     * @param payload JSON string containing the job parameters
     * @throws Exception if the job execution fails
     */
    void execute(String payload) throws Exception;

    /**
     * Get the job type this executor handles.
     *
     * @return the job type
     */
    JobType getJobType();
}
