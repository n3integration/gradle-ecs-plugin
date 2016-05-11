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

import com.amazonaws.services.ecs.AmazonECSClient
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.tasks.TaskAction

/**
 * Responsible for terminating any running services and unregistering task definitions
 *
 * @author n3integration
 */
class DownTask extends DefaultClusterTask implements AutoScaleAware, Ec2Aware {

    DownTask() {
        this.description = "Shuts down running ECS tasks and services"
    }

    @TaskAction
    def downClusterAction() {
        super.execute { AmazonECSClient ecsClient, Cluster _cluster ->
            def instanceSettings = _cluster.instanceSettings
            if(instanceSettings && instanceSettings.autoScaling) {
                instanceSettings.autoScaling.min = 0

                logger.quiet("Scaling back ${cluster} services...")
                scaleServices(ecsClient, _cluster)

                logger.quiet("Deleting ${cluster} services...")
                deleteServices(ecsClient, _cluster)

                logger.quiet("Unregistering ${cluster} tasks...")
                _cluster.families().each { family ->
                    unregisterTasks(ecsClient, family)
                }
            }
        }
    }
}
