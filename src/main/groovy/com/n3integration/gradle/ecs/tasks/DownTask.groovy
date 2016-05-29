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
import com.google.common.base.Strings
import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Container
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

/**
 * Responsible for terminating any running services and unregistering task definitions
 *
 * @author n3integration
 */
class DownTask extends DefaultClusterTask implements AutoScaleAware, Ec2Aware {

    Container container

    DownTask() {
        this.description = "Shuts down running ECS tasks and services"
    }

    @TaskAction
    def downClusterAction() {
        super.execute { AmazonECSClient ecsClient, Cluster _cluster ->
            if(container) {
                def service = container.familySuffix()
                if(Strings.isNullOrEmpty(service)) {
                    throw new GradleException("a container or group name is required")
                }

                logger.quiet("Scaling back ${cluster} services...")
                scaleServices(ecsClient, _cluster.name, Collections.singletonList(service), 0)

                logger.quiet("Deleting ${cluster} services...")
                deleteService(ecsClient, _cluster.name, service)

                unregister(ecsClient, Collections.singletonList("${cluster}-${service}"))
            }
            else {
                def instanceSettings = _cluster.instanceSettings
                if(instanceSettings && instanceSettings.autoScaling) {
                    instanceSettings.autoScaling.min = 0

                    logger.quiet("Scaling back ${cluster} services...")
                    scaleServices(ecsClient, _cluster)

                    logger.quiet("Deleting ${cluster} services...")
                    deleteServices(ecsClient, _cluster)

                    unregister(ecsClient, _cluster.families())
                }
            }
        }
    }

    def container(@DelegatesTo(Container) Closure closure) {
        this.container = new Container()
        def clone = closure.rehydrate(this.container, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    def unregister(AmazonECSClient ecsClient, List<String> families) {
        logger.quiet("Unregistering ${cluster} tasks...")
        families.each { family ->
            unregisterTasks(ecsClient, family)
        }
    }
}
