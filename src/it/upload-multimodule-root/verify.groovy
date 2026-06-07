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

// Verify that the build log contains evidence of the ATR plugin execution
def buildLog = new File(basedir, 'build.log')
assert buildLog.exists()

// The mock client should be called 4 times only in one module
assert buildLog.text.count('[INFO] [Mock ATR client]') == 4:
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [Mock ATR client] uploadFile: tooling-atr-multimodule, 1.0-SNAPSHOT, tooling-atr-multimodule-1.0-SNAPSHOT-source-release.zip,') == 1:
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [Mock ATR client] uploadFile: tooling-atr-multimodule, 1.0-SNAPSHOT, tooling-atr-multimodule-1.0-SNAPSHOT-source-release.zip.sha512,') == 1:
        'Expected message not found in build log'

// The submodules should log a message about skipping the plugin execution
assert buildLog.text.count('Skipping ATR plugin execution (not execution root)') == 2:
        'Expected message not found in build log'

