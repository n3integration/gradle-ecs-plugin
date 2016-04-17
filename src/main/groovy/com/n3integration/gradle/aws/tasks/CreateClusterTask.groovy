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
package com.n3integration.gradle.aws.tasks

import com.amazonaws.services.ecs.model.CreateClusterRequest
import org.gradle.api.tasks.TaskAction

class CreateClusterTask extends DefaultClusterTask {

    CreateClusterTask() {
        this.description = "Creates a new EC2 Container Service cluster"
    }

    @TaskAction
    def createClusterAction() {
        super.execute { ecsClient ->
            logger.quiet("Creating ${clusterName} cluster...")
            def result = ecsClient.createCluster(new CreateClusterRequest().withClusterName(clusterName))
            logger.quiet("${clusterName}:${result.cluster?.status}")
        }
    }
}
