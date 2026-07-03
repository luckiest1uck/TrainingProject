package com.example.trainingproject.filestorage.api;

import java.io.IOException;

public interface BucketIndexMaintenanceApi {

    boolean isEnabled();

    void storeDirectory(String bucketName, String directoryPath) throws IOException;

    void refreshBucketIndex(String bucketName);
}
