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
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.net.URL;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.apache.tooling.atr.client.AtrClient;
import org.apache.tooling.atr.client.AtrClientFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides ATR clients configured from the Maven session: credentials are taken from settings.xml
 * and the JWT is cached in the repository session.
 */
@Named
@Singleton
class AtrClientProvider {

    private final Logger logger = LoggerFactory.getLogger(AtrClientProvider.class);

    private final Provider<MavenSession> mavenSessionProvider;

    private final SettingsDecrypter settingsDecrypter;

    private final AtrClientFactory atrClientFactory;

    @Inject
    AtrClientProvider(
            Provider<MavenSession> mavenSessionProvider,
            SettingsDecrypter settingsDecrypter,
            List<AtrClientFactory> atrClientFactories) {
        this.mavenSessionProvider = mavenSessionProvider;
        this.settingsDecrypter = settingsDecrypter;
        // in test environments, there may be multiple AtrClientFactory implementations,
        // e.g., the default AtrClientFactoryImpl and a mock implementation,
        // so we select the one with the highest priority
        this.atrClientFactory = atrClientFactories.stream()
                .max(Comparator.comparingInt(AtrClientFactory::priority))
                .orElseThrow(() -> new IllegalStateException("No AtrClientFactory implementation found"));
    }

    /**
     * Create a new ATR client.
     * <p>
     * Credentials are taken from the server in settings.xml. If the server or its credentials are missing,
     * the client is created without them and will report the error when authentication is required.
     *
     * @param baseUrl the base URL of the ATR server
     * @param serverId the server ID from settings.xml containing the credentials for the ATR server
     * @return the ATR client
     */
    @SuppressWarnings("unchecked")
    AtrClient createAtrClient(URL baseUrl, String serverId) {
        MavenSession session = mavenSessionProvider.get();
        // retrieve the JWT from the session cache if it exists, otherwise create a new cache entry
        AtomicReference<String> jwtCache = (AtomicReference<String>) session.getRepositorySession()
                .getData()
                .computeIfAbsent(dataKey(baseUrl, serverId), AtomicReference::new);
        Server server = getServer(session, serverId);
        String username = server != null ? server.getUsername() : null;
        String pat = server != null ? server.getPassword() : null;
        return atrClientFactory.createAtrClient(baseUrl, username, pat, jwtCache);
    }

    private Object dataKey(URL baseUrl, String serverId) {
        return String.format("AtrClient-%s-%s", baseUrl, serverId);
    }

    /**
     * Get and decrypt the server configuration.
     *
     * @return the decrypted server, or null if the server is not configured
     */
    private Server getServer(MavenSession session, String serverId) {
        Server server = session.getSettings().getServer(serverId);
        if (server == null) {
            logger.debug("Server '{}' not found in settings.xml", serverId);
            return null;
        }

        DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        SettingsDecryptionResult result = settingsDecrypter.decrypt(request);

        if (!result.getProblems().isEmpty()) {
            logger.warn("Problems decrypting server credentials: {}", result.getProblems());
        }

        return result.getServer();
    }
}
