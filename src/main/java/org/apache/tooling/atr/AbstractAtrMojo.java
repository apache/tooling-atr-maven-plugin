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
import java.util.Comparator;
import java.util.List;

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
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.project", defaultValue = "${project.artifactId}")
    protected String project;

    /**
     * The version for ATR upload.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.version", defaultValue = "${project.version}")
    protected String version;

    /**
     * The ATR server URL.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.url", defaultValue = "https://release-test.apache.org/")
    protected URL url;

    /**
     * Skip plugin execution.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.skip", defaultValue = "false")
    protected boolean skip;

    /**
     * Dry run mode. When enabled, the plugin will simulate execution without performing actual operations.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.dryRun", defaultValue = "false")
    protected boolean dryRun;

    /**
     * Server ID from settings.xml containing ATR credentials.
     * The server's username should be the ASF user ID, and the password should be the Personal Access Token (PAT).
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.serverId", defaultValue = "apache.atr")
    protected String serverId;

    /**
     * If set to true, the plugin will only execute in the execution root directory (typically the top-level
     * directory of a multi-module build). This prevents the plugin from running multiple times in child modules.
     *
     * @since 1.0.0-alpha-1
     */
    @Parameter(property = "atr.runOnlyAtExecutionRoot", defaultValue = "true")
    protected boolean runOnlyAtExecutionRoot;

    private final MavenProject mavenProject;

    private final AtrClientFactory atrClientFactory;

    protected AbstractAtrMojo(MavenProject mavenProject, List<AtrClientFactory> atrClientFactory) {
        this.mavenProject = mavenProject;
        // in test environments, there may be multiple AtrClientFactory implementations,
        // e.g., the default AtrClientFactoryImpl and a mock implementation,
        // so we select the one with the highest priority
        this.atrClientFactory = atrClientFactory.stream()
                .max(Comparator.comparingInt(AtrClientFactory::priority))
                .orElseThrow(() -> new IllegalStateException("No AtrClientFactory implementation found"));
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
