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

// in final log we have both goal executed - prepare and perform
assert buildLog.text.contains('prepare:run-preparation-goals') :
        'Expected message not found in build log'

assert buildLog.text.contains('perform:run-perform-goals') :
        'Expected message not found in build log'

// interaction with ATR client should be visible after perform goal execution
assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] creating new JWT for username: dummy-asfuid')  == 1 :
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] using cached JWT: mock-jwt-dummy-asfuid')  == 1 :
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] created base URL: https://release-test.apache.org/, username: dummy-asfuid, password: dummy-token') == 2 :
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] getRelease: tooling-atr-test-apache-release, 1.0') == 1 :
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] uploadFile: tooling-atr-test-apache-release, 1.0, tooling-atr-test-apache-release-1.0-source-release.zip, ') == 1 :
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [INFO] [Mock ATR client] uploadFile: tooling-atr-test-apache-release, 1.0, tooling-atr-test-apache-release-1.0-source-release.zip.sha512, ') == 1 :
        'Expected message not found in build log'

