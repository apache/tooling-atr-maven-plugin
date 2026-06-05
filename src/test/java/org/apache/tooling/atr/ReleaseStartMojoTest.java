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

import org.apache.maven.api.plugin.testing.InjectMojo;
import org.apache.maven.api.plugin.testing.MojoParameter;
import org.apache.maven.api.plugin.testing.MojoTest;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientFactoryMock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MojoTest(realRepositorySession = true)
class ReleaseStartMojoTest {

    @Inject
    private AtrClientFactoryMock atrClientFactoryMock;

    @Inject
    private MavenSession session;

    @Mock
    private AtrClient atrClient;

    @BeforeEach
    void setup() {
        atrClientFactoryMock.setClient(atrClient);
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "skip", value = "true")
    void skipShouldNotUsedAtrClient(ReleaseStartMojo mojo) throws Exception {
        mojo.execute();
        verifyNoInteractions(atrClient);
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "dryRun", value = "true")
    void dryRunShouldNotUsedAtrClient(ReleaseStartMojo mojo) throws Exception {
        mojo.execute();
        verifyNoInteractions(atrClient);
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "project", value = "test-project")
    void notExistingProjectShouldFailExecution(ReleaseStartMojo mojo) throws Exception {
        setupServerSettings();
        when(atrClient.getProject("test-project")).thenReturn(null);

        MojoFailureException mojoFailureException = assertThrows(MojoFailureException.class, mojo::execute);

        assertEquals("Project 'test-project' does not exist in ATR", mojoFailureException.getMessage());
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "project", value = "test-project")
    void notActiveProjectShouldFailExecution(ReleaseStartMojo mojo) throws Exception {
        setupServerSettings();

        AtrClient.ProjectInfo projectInfo = new AtrClient.ProjectInfo();
        projectInfo.setName("test-project");
        projectInfo.setStatus(AtrClient.ProjectStatus.RETIRED);
        when(atrClient.getProject("test-project")).thenReturn(projectInfo);

        MojoFailureException mojoFailureException = assertThrows(MojoFailureException.class, mojo::execute);

        assertEquals("Project 'test-project' is not active in ATR", mojoFailureException.getMessage());
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "project", value = "test-project")
    @MojoParameter(name = "version", value = "1.0.0")
    void existingReleaseInWrongStateShouldFailExecution(ReleaseStartMojo mojo) throws Exception {
        setupServerSettings();

        AtrClient.ProjectInfo projectInfo = new AtrClient.ProjectInfo();
        projectInfo.setName("test-project");
        projectInfo.setStatus(AtrClient.ProjectStatus.ACTIVE);
        when(atrClient.getProject("test-project")).thenReturn(projectInfo);

        AtrClient.ReleaseInfo releaseInfo = new AtrClient.ReleaseInfo();
        releaseInfo.setProjectName("test-project");
        releaseInfo.setVersion("1.0.0");
        releaseInfo.setPhase(AtrClient.ReleaseInfo.PHASE_RELEASE);
        when(atrClient.getRelease("test-project", "1.0.0")).thenReturn(releaseInfo);

        MojoFailureException mojoFailureException = assertThrows(MojoFailureException.class, mojo::execute);

        assertEquals("Existing version is not being composed: test-project 1.0.0", mojoFailureException.getMessage());
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "project", value = "test-project")
    @MojoParameter(name = "version", value = "1.0.0")
    void existingReleaseInDraftStateShouldNotCreateNextRelease(ReleaseStartMojo mojo) throws Exception {
        setupServerSettings();

        AtrClient.ProjectInfo projectInfo = new AtrClient.ProjectInfo();
        projectInfo.setName("test-project");
        projectInfo.setStatus(AtrClient.ProjectStatus.ACTIVE);
        when(atrClient.getProject("test-project")).thenReturn(projectInfo);

        AtrClient.ReleaseInfo releaseInfo = new AtrClient.ReleaseInfo();
        releaseInfo.setProjectName("test-project");
        releaseInfo.setVersion("1.0.0");
        releaseInfo.setPhase(AtrClient.ReleaseInfo.PHASE_RELEASE_CANDIDATE_DRAFT);
        when(atrClient.getRelease("test-project", "1.0.0")).thenReturn(releaseInfo);

        mojo.execute();

        verify(atrClient, never()).createRelease(anyString(), anyString());
    }

    @Test
    @InjectMojo(goal = "release-start")
    @MojoParameter(name = "project", value = "test-project")
    @MojoParameter(name = "version", value = "1.0.0")
    void releaseShouldBeCreated(ReleaseStartMojo mojo) throws Exception {
        setupServerSettings();

        AtrClient.ProjectInfo projectInfo = new AtrClient.ProjectInfo();
        projectInfo.setName("test-project");
        projectInfo.setStatus(AtrClient.ProjectStatus.ACTIVE);
        when(atrClient.getProject("test-project")).thenReturn(projectInfo);

        // Release does not exist yet
        when(atrClient.getRelease("test-project", "1.0.0")).thenReturn(null);

        AtrClient.ReleaseInfo releaseInfo = new AtrClient.ReleaseInfo();
        releaseInfo.setProjectName("test-project");
        releaseInfo.setVersion("1.0.0");
        releaseInfo.setPhase(AtrClient.ReleaseInfo.PHASE_RELEASE_CANDIDATE_DRAFT);
        when(atrClient.createRelease("test-project", "1.0.0")).thenReturn(releaseInfo);

        mojo.execute();

        verify(atrClient).createRelease("test-project", "1.0.0");
    }

    private void setupServerSettings() {
        Settings settings = new Settings();
        Server server = new Server();
        server.setId("apache.atr");
        server.setUsername("test-user");
        server.setPassword("test-password");
        settings.addServer(server);
        when(session.getSettings()).thenReturn(settings);
    }
}
