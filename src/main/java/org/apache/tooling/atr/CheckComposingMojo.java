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

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientException;
import org.apache.tooling.atr.client.AtrClientFactory;

/**
 * Check if a version exists in ATR and is being composed (in draft phase).
 *
 * @author Maven Team
 * @since 1.0.0-alpha-1
 */
@Mojo(name = "check-composing", defaultPhase = LifecyclePhase.INITIALIZE, threadSafe = true)
public class CheckComposingMojo extends AbstractAtrMojo {

    /**
     * If set to true, the plugin will create the version in ATR if it does not exist.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.createVersion", defaultValue = "false")
    private boolean createVersion;

    @Inject
    CheckComposingMojo(MavenProject mavenProject, List<AtrClientFactory> atrClientFactories) {
        super(mavenProject, atrClientFactories);
    }

    @Override
    protected void atrExecute() throws MojoFailureException, AtrClientException {
        if (dryRun) {
            getLog().info("DRY RUN: Simulating ATR version check (no actual API call will be made)");
        }

        getLog().info("Checking project release in ATR: " + url + "projects/" + project + " version " + version);

        if (dryRun) {
            getLog().info("DRY RUN: Would check version at " + url + "api/release/get/" + project + "/" + version);
            return;
        }

        AtrClient client = createAtrClient();
        AtrClient.ReleaseInfo releaseInfo = client.getRelease(project, version);

        if (releaseInfo == null && createVersion) {
            getLog().info("Version does not exist in ATR, creating new version: " + project + " " + version);
            releaseInfo = client.createRelease(project, version);
        }

        if (releaseInfo == null) {
            getLog().info("Version does not exist in ATR: " + project + " " + version + System.lineSeparator()
                    + "       See " + url + "projects/" + project + " for more information");
            throw new MojoFailureException("Version does not exist in ATR: " + project + " " + version);
        }

        getLog().info("Version exists in ATR:" + System.lineSeparator()
                + "         Phase: " + releaseInfo.getPhaseDescription() + System.lineSeparator()
                + "         Created: " + releaseInfo.getCreated() + System.lineSeparator()
                + "         Latest Revision: " + releaseInfo.getLatestRevisionNumber());

        // Check fail conditions
        if (releaseInfo.isComposing()) {
            getLog().info("Version is being composed in ATR: " + project + " " + version + System.lineSeparator()
                    + "       See " + url + "compose/" + project + "/" + version + " for more information");
        } else {
            if (releaseInfo.isReleased()) {
                getLog().error("Version is already released: " + project + " " + version + System.lineSeparator()
                        + "       See " + url + "file/" + project + "/" + version + " for more information");
            } else {
                getLog().error("Version is " + releaseInfo.getPhaseDescription() + ": " + project + " " + version
                        + System.lineSeparator() + "       See " + url + "compose/" + project + "/" + version
                        + " for more information");
            }
            throw new MojoFailureException("Version is not being composed: " + project + " " + version);
        }
    }
}
