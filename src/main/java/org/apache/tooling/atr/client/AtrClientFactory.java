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

import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Factory for creating ATR clients.
 */
public interface AtrClientFactory {

    /**
     * The priority of this factory. Mojos will use higher priority factories first.
     *
     * @return the priority of this factory
     */
    int priority();

    /**
     * Create a new ATR client.
     *
     * @param baseUrl the base URL of the ATR server
     * @param username the ASF user ID
     * @param pat the ATR Personal Access Token (PAT)
     * @param jwtCache the reference for caching JWT across clients
     * @return the ATR client
     */
    AtrClient createAtrClient(URL baseUrl, String username, String pat, AtomicReference<String> jwtCache);
}
