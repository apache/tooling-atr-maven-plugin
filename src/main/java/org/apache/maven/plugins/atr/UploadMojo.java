/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.maven.plugins.atr;

import java.nio.file.Path;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Upload Apache distribution artifacts to ATR (Apache Test Release) compose space before vote.
 *
 * @author Maven Team
 */
@Mojo(name = "upload", requiresProject = true, defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class UploadMojo extends AbstractAtrMojo {

    /**
     * The project name for ATR upload.
     */
    @Parameter(property = "atr.project", required = true)
    private String project;

    /**
     * The version for ATR upload.
     */
    @Parameter(property = "atr.version", required = true)
    private String version;

    /**
     * The files to upload to ATR.
     */
    @Parameter(property = "atr.files", required = true)
    private Path[] files;

    /**
     * The target directory in ATR upload area where files will be uploaded.
     * If not specified, files will be uploaded to the default location.
     */
    @Parameter(property = "atr.directory")
    private String directory;

    @Override
    protected void atrExecute() throws MojoExecutionException, MojoFailureException {
        if (dryRun) {
            getLog().info("DRY RUN: Simulating ATR upload (no actual upload will occur)");
        } else {
            getLog().info("Uploading artifacts to ATR...");
        }
        getLog().info("Composing release " + url + "compose/" + project + "/" + version);
        if (directory != null) {
            getLog().info("Directory: " + directory);
        }
        getLog().info("Files: " + files.length);

        for (Path file : files) {
            upload(file);
        }
    }

    /**
     * Upload a single file to ATR.
     *
     * @param file the file to upload
     * @throws MojoExecutionException if an error occurs during upload
     * @throws MojoFailureException if the upload fails
     */
    private void upload(Path file) throws MojoExecutionException, MojoFailureException {
        if (dryRun) {
            getLog().info("DRY RUN: Would upload: " + file.getFileName() + " to " + getAtrFileUrl(file));
            return;
        }

        getLog().info("Uploading: " + file.getFileName() + " to " + getAtrFileUrl(file));

        // Build target path on ATR space
        String target =
                (directory != null ? directory + "/" : "") + file.getFileName().toString();

        // Upload using ATR client
        AtrClient client = new AtrClient(url, token, asfuid, getLog());
        String revisionNumber = client.uploadFile(project, version, target, file);

        getLog().info("Upload successful. Revision: " + revisionNumber);
    }

    /**
     * Get the ATR storage URL for a file.
     *
     * @param file the file to get the ATR file URL for
     * @return the file URL
     */
    private String getAtrFileUrl(Path file) {
        StringBuilder path = new StringBuilder(url.toString())
                .append("file/")
                .append(project)
                .append("/")
                .append(version)
                .append("/");
        if (directory != null) {
            path.append(directory).append("/");
        }
        path.append(file.getFileName().toString());
        return path.toString();
    }
}
