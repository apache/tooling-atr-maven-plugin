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
import org.apache.maven.settings.crypto.SettingsDecrypter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock factory for creating ATR clients for testing purposes.
 * This factory will be used in tests instead of the real AtrClientFactoryImpl,
 * and it will create AtrClientMock instances instead of real AtrClientImpl instances.
 */
@Named
@Singleton
public class AtrClientFactoryMock extends AtrClientFactoryImpl {

    private final Logger logger = LoggerFactory.getLogger(AtrClientFactoryMock.class);

    private AtrClient atrClient;

    @Inject
    AtrClientFactoryMock(Provider<MavenSession> mavenSessionProvider, SettingsDecrypter settingsDecrypter) {
        super(mavenSessionProvider, settingsDecrypter);
        logger.info("[Mock ATR client factory] created");
    }

    @Override
    public int priority() {
        return 10;
    }

    @Override
    protected AtrClient newAtrClient(URL baseUrl, String username, String password, AtomicReference<String> jwtCache) {
        if (atrClient != null) {
            return atrClient;
        }
        return new AtrClientMock(baseUrl, username, password, jwtCache);
    }

    /**
     * Set the ATR client to be returned by this factory.
     *
     * @param atrClient the ATR client to be returned by this factory
     */
    public void setClient(AtrClient atrClient) {
        this.atrClient = atrClient;
    }
}
