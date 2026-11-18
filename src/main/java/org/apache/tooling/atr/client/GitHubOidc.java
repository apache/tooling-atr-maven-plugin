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
package org.apache.tooling.atr.client;

import java.util.Optional;

/**
 * Access to the GitHub Actions OIDC token request, used for Trusted Publishing.
 * <p>
 * GitHub provides the request URL and token in environment variables when the job has
 * the <code>id-token: write</code> permission.
 */
public final class GitHubOidc {

    private static final String REQUEST_URL_ENV = "ACTIONS_ID_TOKEN_REQUEST_URL";

    private static final String REQUEST_TOKEN_ENV = "ACTIONS_ID_TOKEN_REQUEST_TOKEN";

    private final String requestUrl;

    private final String requestToken;

    GitHubOidc(String requestUrl, String requestToken) {
        this.requestUrl = requestUrl;
        this.requestToken = requestToken;
    }

    /**
     * Get the GitHub OIDC token request from the environment.
     *
     * @return the GitHub OIDC token request, or empty if not running in GitHub Actions with OIDC enabled
     */
    public static Optional<GitHubOidc> fromEnvironment() {
        String requestUrl = System.getenv(REQUEST_URL_ENV);
        String requestToken = System.getenv(REQUEST_TOKEN_ENV);
        if (requestUrl == null || requestUrl.isEmpty() || requestToken == null || requestToken.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(new GitHubOidc(requestUrl, requestToken));
    }

    String getRequestUrl() {
        return requestUrl;
    }

    String getRequestToken() {
        return requestToken;
    }
}
