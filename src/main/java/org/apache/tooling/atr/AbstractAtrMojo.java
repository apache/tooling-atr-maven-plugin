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

import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientException;
import org.apache.tooling.atr.client.AtrClientFactory;

/**
 * Abstract base class for ATR Mojos.
 *
 * @author Maven Team
 */
public abstract class AbstractAtrMojo extends AbstractMojo {

    /**
     * The project key for ATR upload.
     */
    @Parameter(property = "atr.project", defaultValue = "${project.artifactId}")
    protected String project;

    /**
     * The version for ATR upload.
     */
    @Parameter(property = "atr.version", defaultValue = "${project.version}")
    protected String version;

    /**
     * The ATR server URL.
     */
    @Parameter(property = "atr.url", defaultValue = "https://release-test.apache.org/")
    protected URL url;

    /**
     * Skip plugin execution.
     */
    @Parameter(property = "atr.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Dry run mode. When enabled, the plugin will simulate execution without performing actual operations.
     */
    @Parameter(property = "atr.dryRun", defaultValue = "false")
    protected boolean dryRun;

    /**
     * Server ID from settings.xml containing ATR credentials.
     * The server's username should be the ASF user ID, and the password should be the Personal Access Token (PAT).
     */
    @Parameter(property = "atr.serverId", defaultValue = "apache.atr")
    protected String serverId;

    /**
     * If set to true, the plugin will only execute in the execution root directory (typically the top-level
     * directory of a multi-module build). This prevents the plugin from running multiple times in child modules.
     */
    @Parameter(property = "atr.runOnlyAtExecutionRoot", defaultValue = "false")
    protected boolean runOnlyAtExecutionRoot;

    private final MavenProject mavenProject;

    private final AtrClientFactory atrClientFactory;

    protected AbstractAtrMojo(MavenProject mavenProject, AtrClientFactory atrClientFactory) {
        this.mavenProject = mavenProject;
        this.atrClientFactory = atrClientFactory;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping ATR plugin execution");
            return;
        }

        if (runOnlyAtExecutionRoot && !mavenProject.isExecutionRoot()) {
            getLog().info("Skipping ATR plugin execution (not execution root)");
            return;
        }

        try {
            atrExecute();
        } catch (AtrClientException e) {
            throw new MojoExecutionException(e);
        }
    }

    /**
     * Create an ATR client with JWT caching support.
     *
     * @return the ATR client
     * @throws AtrClientException if client creation fails
     */
    protected AtrClient createAtrClient() throws AtrClientException {
        return atrClientFactory.createAtrClient(url, serverId);
    }

    /**
     * Execute the ATR-specific logic.
     *
     * @throws MojoExecutionException if an error occurs during execution
     * @throws MojoFailureException if a failure occurs during execution
     * @throws AtrClientException if a failure in the ATR client occurs
     */
    protected abstract void atrExecute() throws MojoExecutionException, MojoFailureException, AtrClientException;
}
