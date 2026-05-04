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

assert buildLog.text.contains('[INFO] Checking project release in ATR: https://release-test.apache.org/projects/maven-atr-test-apache-release version 1.0-SNAPSHOT') :
        'Expected message not found in build log'

assert buildLog.text.contains('to https://release-test.apache.org/file/maven-atr-test-apache-release/1.0-SNAPSHOT/maven-atr-test-apache-release-1.0-SNAPSHOT-source-release.zip') :
        'Expected message not found in build log'

assert buildLog.text.contains('to https://release-test.apache.org/file/maven-atr-test-apache-release/1.0-SNAPSHOT/maven-atr-test-apache-release-1.0-SNAPSHOT-source-release.zip.sha512') :
        'Expected message not found in build log'

return true
