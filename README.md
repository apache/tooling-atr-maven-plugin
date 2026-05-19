<!---
Licensed to the Apache Software Foundation (ASF) under one
or more contributor license agreements.  See the NOTICE file
distributed with this work for additional information
regarding copyright ownership.  The ASF licenses this file
to you under the Apache License, Version 2.0 (the
"License"); you may not use this file except in compliance
with the License.  You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing,
software distributed under the License is distributed on an
"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
KIND, either express or implied.  See the License for the
specific language governing permissions and limitations
under the License.
-->

# Tooling ATR Maven Plugin

ATR integration for Maven.

It provides a Maven plugin with an [`atr:upload` goal](src/site/markdown/usage.md) to [upload Apache distribution artifacts](https://release-test.apache.org/tutorial) to ATR before vote, like [`atr upload` CLI](https://github.com/apache/tooling-releases-client/blob/main/COMMANDS.md#atr-upload).

This can be triggered during `mvn deploy` in an `atr` profile along `apache-release` one: see IT `mvn -Prun-its verify` for demo.
