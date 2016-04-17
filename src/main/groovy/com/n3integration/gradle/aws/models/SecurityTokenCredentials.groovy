/*
 *  Copyright 2016 n3integration
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package com.n3integration.gradle.aws.models

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicSessionCredentials

class SecurityTokenCredentials extends Credentials {

    String roleArn
    String sessionToken
    String roleSessionName = "session"

    def AWSCredentials toCredentials() {
        if(sessionToken) {
            new BasicSessionCredentials(this.accessKey, this.secretKey, this.sessionToken)
        }
        else {
            super.toCredentials()
        }
    }
}
