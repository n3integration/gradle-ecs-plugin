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
package com.n3integration.gradle.ecs.models

import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicSessionCredentials
import com.n3integration.gradle.ecs.RoleAware

/**
 * Allows users to define role-based credentials within their build.gradle files
 *
 * @author n3integration
 */
class SecurityTokenCredentials extends Credentials implements RoleAware {

    String roleArn
    String endpoint = "sts.us-east-1.amazonaws.com"
    String sessionName = "session"

    def AWSCredentials toCredentials() {
        if(roleArn) {
            def credentials = assumeRole(this)
            new BasicSessionCredentials(credentials.accessKeyId, credentials.secretAccessKey, credentials.sessionToken)
        }
        else {
            super.toCredentials()
        }
    }
}
