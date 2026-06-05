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

import java.util.List;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientException;
import org.apache.tooling.atr.client.AtrClientFactory;

/**
 * Start a release in ATR.
 * <p>
 * Create a composed (in draft phase) version in ATR if it does not exist yet.
 *
 * @since 1.0.0-beta-1
 */
@Mojo(name = "release-start", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class ReleaseStartMojo extends AbstractAtrMojo {

    @Inject
    protected ReleaseStartMojo(MavenProject mavenProject, List<AtrClientFactory> atrClientFactory) {
        super(mavenProject, atrClientFactory);
    }

    @Override
    protected void atrExecute() throws MojoExecutionException, MojoFailureException, AtrClientException {

        if (dryRun) {
            getLog().info("DRY RUN: Simulating ATR release start (no actual API call will be made)");
        }

        getLog().info("Starting release in ATR: " + url + "projects/" + project + " version " + version);

        if (dryRun) {
            getLog().info("DRY RUN: Would create release at " + url + "api/release/create/" + project + "/" + version);
            return;
        }

        AtrClient client = createAtrClient();

        AtrClient.ProjectInfo atrProject = client.getProject(project);
        if (atrProject == null) {
            throw new MojoFailureException("Project '" + project + "' does not exist in ATR");
        }

        if (atrProject.getStatus() != AtrClient.ProjectStatus.ACTIVE) {
            getLog().error("Project is not active in ATR: " + project + " (status: " + atrProject.getStatus() + ")"
                    + System.lineSeparator() + "       See " + url + "projects/" + project
                    + " for more information");
            throw new MojoFailureException("Project '" + project + "' is not active in ATR");
        }

        AtrClient.ReleaseInfo releaseInfo = client.getRelease(project, version);
        if (releaseInfo != null) {
            if (releaseInfo.isComposing()) {
                getLog().info("Release is already being composed in ATR: " + project + " " + version
                        + " created: " + releaseInfo.getCreated()
                        + System.lineSeparator() + "       See " + url + "compose/" + project + "/" + version
                        + " for more information");
            } else {
                getLog().error("Version is " + releaseInfo.getPhaseDescription() + ": " + project + " " + version
                        + System.lineSeparator() + "       See " + url + "compose/" + project + "/" + version
                        + " for more information");
                throw new MojoFailureException("Existing version is not being composed: " + project + " " + version);
            }
        } else {
            releaseInfo = client.createRelease(project, version);
            getLog().info("Release started in ATR: " + project + " " + version
                    + " created: " + releaseInfo.getCreated() + System.lineSeparator()
                    + "       See " + url + "compose/" + project + "/" + version + " for more information");
        }
    }
}
