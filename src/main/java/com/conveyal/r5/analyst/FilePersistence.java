package com.conveyal.r5.analyst;

import com.conveyal.r5.analyst.cluster.AnalysisTask;

import java.io.OutputStream;

/**
 * This is an abstraction for long term file storage.
 * We used to use only S3 for this, but we now provide an abstract class to allow multiple implementations.
 * Files saved by an implementation should be available to both the backend and the workers indefinitely into the future.
 */
public abstract class FilePersistence {

    /**
     * This is a blocking call and should only return when the file is completely uploaded.
     * That prevents our workers from producing output faster than uploads can complete,
     * and building up a queue of waiting uploads.
     * The PersistenceBuffer must be marked 'done' before it is handed to this method.
     */
    public abstract void saveData (String directory, String fileName, PersistenceBuffer persistenceBuffer);

    /**
     * This should be called when the application is shutting down to perform any cleanup, await completion,
     * shutdown async upload threads etc.
     */
    public abstract void shutdown();

}
