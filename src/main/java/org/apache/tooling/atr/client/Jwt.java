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

import java.time.Instant;

/**
 * ATR JWT with its expiry time.
 */
public final class Jwt {

    /**
     * Time before the expiry of the JWT when a new one should be issued.
     */
    private static final long EXPIRY_MARGIN_SECONDS = 30;

    private final String token;

    private final long expires;

    /**
     * @param token the JWT
     * @param expires the expiry time in epoch seconds
     */
    Jwt(String token, long expires) {
        this.token = token;
        this.expires = expires;
    }

    String getToken() {
        return token;
    }

    long getExpires() {
        return expires;
    }

    /**
     * Check if the JWT can still be used, it is not about to expire.
     *
     * @return true if the JWT can be used
     */
    boolean isValid() {
        return Instant.now().getEpochSecond() < expires - EXPIRY_MARGIN_SECONDS;
    }
}
