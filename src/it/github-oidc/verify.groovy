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

def commit = ['git', 'rev-parse', 'HEAD'].execute(null, basedir).text.trim()

assert buildLog.text.count('[INFO] Using GitHub OIDC token (Trusted Publishing) for ATR authentication') == 2:
        'Expected message not found in build log'

// check-composing does not give the source commit
assert buildLog.text.count('[INFO] [Mock ATR client] created base URL: https://releases.apache.org/, GitHub OIDC request URL: https://token.actions.example/oidc?api-version=2.0, source commit: null') == 1:
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [Mock ATR client] created base URL: https://releases.apache.org/, GitHub OIDC request URL: https://token.actions.example/oidc?api-version=2.0, source commit: ' + commit) == 1:
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [Mock ATR client] getRelease: tooling-atr-github-oidc, 1.0-SNAPSHOT') == 1:
        'Expected message not found in build log'

// with GitHub OIDC the source commit is not set with a separate call
assert buildLog.text.count('[Mock ATR client] setSourceCommit') == 0:
        'Unexpected message found in build log'

assert buildLog.text.count('[INFO] Source commit recorded in ATR with uploaded files: ' + commit) == 1:
        'Expected message not found in build log'

assert buildLog.text.count('[INFO] [Mock ATR client] uploadFile: tooling-atr-github-oidc, 1.0-SNAPSHOT, pom.xml, ') == 1:
        'Expected message not found in build log'

return true
