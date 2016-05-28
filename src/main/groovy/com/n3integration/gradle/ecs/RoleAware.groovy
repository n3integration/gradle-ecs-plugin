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
package com.n3integration.gradle.ecs

import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClient
import com.amazonaws.services.securitytoken.model.AssumeRoleRequest
import com.amazonaws.services.securitytoken.model.Credentials
import com.n3integration.gradle.ecs.models.SecurityTokenCredentials

/**
 * Trait responsible for interacting with the SecurityTokenService
 *
 * @author n3integration
 */
trait RoleAware {

    /**
     * Temporarily grants AWS access using a predefined role
     *
     * @param credentials
     *          the {@link SecurityTokenCredentials}
     * @return the {@link Credentials}
     */
    def Credentials assumeRole(SecurityTokenCredentials credentials) {
        def client = createStsClient(credentials.endpoint)
        def result = client.assumeRole(new AssumeRoleRequest()
            .withRoleArn(credentials.roleArn)
            .withRoleSessionName(credentials.sessionName))
        result.credentials
    }

    /*
     * Initializes a new {@link AWSSecurityTokenServiceClient} instance
     *
     * @param endpoint
     *          the STS endpoint URL
     * @return a new {@link AWSSecurityTokenServiceClient}
     */
    private AWSSecurityTokenServiceClient createStsClient(String endpoint) {
        def client = new AWSSecurityTokenServiceClient()
        client.setEndpoint(endpoint)
        client
    }
}