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
import com.n3integration.gradle.ecs.ECSAware
import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Container
import org.gradle.api.tasks.TaskAction

import java.nio.file.Files

/**
 * Responsible for registering task definitions, creating services, and starting
 * the registered tasks.
 *
 * @author n3integration
 */
class Up extends DefaultClusterTask implements ECSAware {

    Up() {
        this.description = "Creates the ECS task definition, service, and starts the containers, if necessary"
    }

    @TaskAction
    def upAction() {
        Files.createDirectories(taskDefDir.toPath())
        super.execute { AmazonECSClient ecsClient, Cluster cluster ->
            cluster.containers.groupBy { container ->
                container.familySuffix()
            }.each { familySuffix, containers ->
                logger.quiet("Registering task definition ${familySuffix}...")
                def taskDef = createTaskDefinition(ecsClient, cluster, familySuffix, containers)
                logger.quiet("\t  family: ${taskDef.family}")
                logger.quiet("\trevision: ${taskDef.revision}")
                logger.quiet("\t  status: ${taskDef.status}")

                logger.quiet("Creating ${familySuffix} service...")
                def count =  getInstanceCount(containers)
                def service = createService(ecsClient, cluster, familySuffix, count, taskDef)
                logger.quiet("\t   arn: ${service.serviceArn}")
                logger.quiet("\tstatus: ${service.status}")

                logger.quiet("Starting ${cluster.name} tasks...")
                def tasks = startTasks(ecsClient, cluster, count, taskDef)
                tasks.each { task ->
                    logger.quiet("\t   created: ${task.createdAt}")
                    logger.quiet("\t    status: ${task.lastStatus}")
                    logger.quiet("\tcontainers: ${task.containers}")
                }
            }
        }
    }

    def int getInstanceCount(List<Container> containers) {
        containers.collect { container ->
            if(container.instances > 0) {
                container.instances
            }
            else {
                1
            }
        }.inject(0) { sum, i ->
            sum + i
        }
    }
}
