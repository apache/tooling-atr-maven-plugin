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

import java.net.MalformedURLException;
import java.net.URL;
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
        getLog().info("Project: " + project);
        getLog().info("Version: " + version);
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
            getLog().info("DRY RUN: Would upload: " + file.getFileName() + " to " + getComposeUrl(file));
            return;
        }

        getLog().info("Uploading: " + file.getFileName() + " to " + getComposeUrl(file));

        // TODO: Implement ATR upload logic for single file
        // This will integrate with the ATR CLI (atr upload) functionality
        // to upload Apache distribution artifacts to release-test.apache.org
    }

    /**
     * Get the compose URL for a file by combining the base URL, project, version, directory, and filename.
     *
     * @param file the file to get the compose URL for
     * @return the compose URL
     * @throws MojoExecutionException if the URL cannot be constructed
     */
    private URL getComposeUrl(Path file) throws MojoExecutionException {
        StringBuilder path = new StringBuilder("file/")
                .append(project)
                .append("/")
                .append(version)
                .append("/");
        if (directory != null) {
            path.append(directory).append("/");
        }
        path.append(file.getFileName().toString());
        try {
            return new URL(url, path.toString());
        } catch (MalformedURLException e) {
            throw new MojoExecutionException("Failed to construct compose URL for file: " + file, e);
        }
    }
}
