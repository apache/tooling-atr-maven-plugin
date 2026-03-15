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
import org.apache.maven.plugins.annotations.Parameter;

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
     * Personal Access Token (PAT) for ATR API authentication.
     */
    @Parameter(property = "atr.token", required = true)
    protected String token;

    /**
     * ASF user ID for ATR API authentication.
     */
    @Parameter(property = "atr.asfuid", required = true)
    protected String asfuid;

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
