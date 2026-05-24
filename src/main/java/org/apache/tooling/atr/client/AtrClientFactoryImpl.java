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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.maven.execution.MavenSession;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.crypto.DefaultSettingsDecryptionRequest;
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.apache.maven.settings.crypto.SettingsDecryptionResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating ATR clients.
 */
@Named
@Singleton
public class AtrClientFactoryImpl implements AtrClientFactory {

    private final Logger logger = LoggerFactory.getLogger(AtrClientFactoryImpl.class);

    private final Provider<MavenSession> mavenSessionProvider;

    private final SettingsDecrypter settingsDecrypter;

    @Inject
    AtrClientFactoryImpl(Provider<MavenSession> mavenSessionProvider, SettingsDecrypter settingsDecrypter) {
        this.mavenSessionProvider = mavenSessionProvider;
        this.settingsDecrypter = settingsDecrypter;
    }

    @Override
    public int priority() {
        return -10;
    }

    /**
     * Create a new ATR client.
     *
     * @param baseUrl the base URL of the ATR server
     * @param serverId the server ID from settings.xml containing the credentials for the ATR server
     * @return the ATR client
     * @throws AtrClientException if the client cannot be created
     */
    @SuppressWarnings("unchecked")
    public AtrClient createAtrClient(URL baseUrl, String serverId) throws AtrClientException {
        MavenSession session = mavenSessionProvider.get();
        // retrieve the JWT from the session cache if it exists, otherwise create a new cache entry
        AtomicReference<String> jwtCache = (AtomicReference<String>) session.getRepositorySession()
                .getData()
                .computeIfAbsent(dataKey(baseUrl, serverId), AtomicReference::new);
        Server server = getServer(session, serverId);
        return newAtrClient(baseUrl, server.getUsername(), server.getPassword(), jwtCache);
    }

    protected AtrClient newAtrClient(URL baseUrl, String username, String password, AtomicReference<String> jwtCache) {
        return new AtrClientImpl(baseUrl, username, password, jwtCache);
    }

    private Object dataKey(URL baseUrl, String serverId) {
        return String.format("AtrClient-%s-%s", baseUrl, serverId);
    }

    /**
     * Get and decrypt the server configuration.
     *
     * @return the decrypted server
     * @throws AtrClientException if the server cannot be found or decrypted
     */
    private Server getServer(MavenSession session, String serverId) throws AtrClientException {
        Server server = session.getSettings().getServer(serverId);
        if (server == null) {
            logger.error("Missing permissions for '{}' server in ~/.m2/settings.xml", serverId);
            throw new AtrClientException(
                    "<server><id>" + serverId + "</id> not found in ~/.m2/settings.xml. "
                            + "Please configure it with your ASF user ID as <username> and ATR Personal Access Token as <password> (encrypted if enabled).");
        }

        DefaultSettingsDecryptionRequest request = new DefaultSettingsDecryptionRequest(server);
        SettingsDecryptionResult result = settingsDecrypter.decrypt(request);

        if (!result.getProblems().isEmpty()) {
            logger.warn("Problems decrypting server credentials: " + result.getProblems());
        }

        server = result.getServer();
        if (server.getUsername() == null || server.getPassword() == null) {
            throw new AtrClientException("Server '" + serverId
                    + "' must have username (ASF user ID) and password (ATR PAT) configured in settings.xml.");
        }

        return server;
    }
}
