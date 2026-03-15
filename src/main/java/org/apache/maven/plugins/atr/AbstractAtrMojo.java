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

import java.net.URL;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;

/**
 * Abstract base class for ATR Mojos.
 *
 * @author Maven Team
 */
public abstract class AbstractAtrMojo extends AbstractMojo {

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
     * Maven settings.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    /**
     * Settings decrypter component.
     */
    @Component
    protected SettingsDecrypter settingsDecrypter;

    /**
     * Get and decrypt the server configuration.
     *
     * @return the decrypted server
     * @throws MojoExecutionException if server cannot be found or decrypted
     */
    protected Server getServer() throws MojoExecutionException {
        Server server = settings.getServer(serverId);
        if (server == null) {
            throw new MojoExecutionException("Server '" + serverId + "' not found in settings.xml. "
                    + "Please configure it with your ASF user ID as username and PAT as password.");
        }

        DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        SettingsDecryptionResult result = settingsDecrypter.decrypt(request);

        if (!result.getProblems().isEmpty()) {
            getLog().warn("Problems decrypting server credentials: " + result.getProblems());
        }

        server = result.getServer();
        if (server.getUsername() == null || server.getPassword() == null) {
            throw new MojoExecutionException("Server '" + serverId
                    + "' must have username (ASF user ID) and password (PAT) configured in settings.xml.");
        }

        return server;
    }

    @Override
    public final void execute() throws MojoExecutionException, MojoFailureException {
        if (skip) {
            getLog().info("Skipping ATR plugin execution");
            return;
        }

        atrExecute();
    }

    /**
     * Execute the ATR-specific logic.
     *
     * @throws MojoExecutionException if an error occurs during execution
     * @throws MojoFailureException if a failure occurs during execution
     */
    protected abstract void atrExecute() throws MojoExecutionException, MojoFailureException;
}
