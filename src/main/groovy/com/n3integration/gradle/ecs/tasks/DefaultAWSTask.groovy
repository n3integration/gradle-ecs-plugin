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
package com.n3integration.gradle.ecs.tasks

import com.amazonaws.auth.AWSCredentials
import com.n3integration.gradle.ecs.AWSAware
import org.gradle.api.DefaultTask
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Base implementation for AWS related tasks
 */
class DefaultAWSTask extends DefaultTask implements AWSAware {

    private static Logger logger = Logging.getLogger(DefaultAWSTask)

    AWSCredentials credentials

    /**
     * Retrieves the {@link AWSCredentials} for the current session. Either
     * @{code ecs.credentials}, {@code ecs.securityTokenCredentials} or
     * default credentials.
     *
     * @return the {@link AWSCredentials}
     */
    def AWSCredentials getCredentials() {
        if(credentials == null) {
            if(project.ecs.credentials) {
                logger.info("Found credentials.")
                credentials = project.ecs.credentials.toCredentials()
            }
            else if(project.ecs.securityTokenCredentials) {
                logger.info("Found STS credentials.")
                credentials = project.ecs.securityTokenCredentials.toCredentials()
            }
            else {
                logger.info("Resolving default credentials")
                credentials = defaultCredentials()
            }
        }
        credentials
    }
}
