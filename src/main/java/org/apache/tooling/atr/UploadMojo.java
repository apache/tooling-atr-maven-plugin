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
package org.apache.tooling.atr;

import javax.inject.Inject;

import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.scm.ScmException;
import org.apache.maven.scm.ScmFileSet;
import org.apache.maven.scm.command.info.InfoItem;
import org.apache.maven.scm.command.info.InfoScmResult;
import org.apache.maven.scm.manager.ScmManager;
import org.apache.maven.scm.repository.ScmRepository;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientException;
import org.codehaus.plexus.util.FileUtils;

/**
 * Upload Apache distribution artifacts to ATR (Apache Test Release) compose space before vote.
 *
 * @author Maven Team
 * @since 1.0.0-alpha-1
 */
@Mojo(name = "upload", defaultPhase = LifecyclePhase.DEPLOY, threadSafe = true)
public class UploadMojo extends AbstractAtrMojo {

    /**
     * The files to upload to ATR.
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.files", required = true)
    private Path[] files;

    /**
     * The target directory in ATR upload area where files will be uploaded.
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.directory")
    private String directory;

    private final ScmManager scmManager;

    @Inject
    UploadMojo(MavenProject mavenProject, AtrClientProvider atrClientProvider, ScmManager scmManager) {
        super(mavenProject, atrClientProvider);
        this.scmManager = scmManager;
    }

    @Override
    protected void atrExecute() throws AtrClientException {
        if (dryRun) {
            getLog().info("DRY RUN: Simulating ATR upload (no actual upload will occur)");
        } else {
            getLog().info("Uploading " + files.length + " files to ATR " + url + "compose/" + project + "/" + version);
        }

        logSourceCommit();

        AtrClient client = createAtrClient();

        for (Path file : files) {
            upload(client, file);
        }
    }

    /**
     * Upload a single file to ATR.
     *
     * @param client the ATR client to use for upload
     * @param file the file to upload
     */
    private void upload(AtrClient client, Path file) throws AtrClientException {
        if (dryRun) {
            getLog().info("DRY RUN: Would upload: " + file.getFileName() + " to " + getAtrFileUrl(file));
            return;
        }

        getLog().info(">>> " + file.getFileName() + " ("
                + FileUtils.byteCountToDisplaySize((int) file.toFile().length()) + ") to " + getAtrFileUrl(file));

        // Build target path on ATR space
        String target =
                (directory != null ? directory + "/" : "") + file.getFileName().toString();

        // Upload using ATR client
        String revisionNumber = client.uploadFile(project, version, target, file);

        getLog().info("    upload successful, revision: " + revisionNumber);
    }

    /**
     * Log the source commit of the project.
     */
    private void logSourceCommit() {
        getLog().info("Source commit: " + getSourceCommit().orElse("unknown"));
    }

    /**
     * Get the source commit (SCM revision) of the project base directory.
     *
     * @return the source commit, or empty if it can not be determined
     */
    private Optional<String> getSourceCommit() {
        File basedir = getMavenProject().getBasedir();
        Optional<ScmRepository> repository = findScmRepository();
        if (!repository.isPresent()) {
            getLog().warn(basedir + " is not in a SCM working directory");
            return Optional.empty();
        }

        try {
            InfoScmResult result = scmManager
                    .getProviderByRepository(repository.get())
                    .info(repository.get().getProviderRepository(), new ScmFileSet(basedir), null);
            List<InfoItem> infoItems = result.getInfoItems();
            if (!result.isSuccess() || infoItems.isEmpty()) {
                getLog().warn("Can not get SCM info: " + result.getProviderMessage());
                return Optional.empty();
            }
            return Optional.ofNullable(infoItems.get(0).getRevision());
        } catch (ScmException e) {
            getLog().warn("Can not get SCM info: " + e.getMessage(), e);
            return Optional.empty();
        }
    }

    /**
     * Find the SCM repository of the project, also looking in the base directories of local parent projects,
     * as the SCM working directory root (e.g. <code>.git</code>) may be in one of them.
     *
     * @return the SCM repository, or empty if none found
     */
    private Optional<ScmRepository> findScmRepository() {
        for (MavenProject p = getMavenProject(); p != null && p.getBasedir() != null; p = p.getParent()) {
            Optional<ScmRepository> repository = scmManager.makeProviderScmRepository(p.getBasedir());
            if (repository.isPresent()) {
                return repository;
            }
        }
        return Optional.empty();
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
