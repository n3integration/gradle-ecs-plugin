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
import com.amazonaws.services.ecs.model.CreateServiceRequest
import com.amazonaws.services.ecs.model.RegisterTaskDefinitionRequest
import com.amazonaws.services.ecs.model.StartTaskRequest
import com.amazonaws.services.ecs.model.TaskDefinition
import com.n3integration.gradle.ecs.models.Cluster
import com.n3integration.gradle.ecs.models.Container
import org.gradle.api.GradleException
import org.gradle.api.tasks.TaskAction

class EcsUpTask extends DefaultClusterTask {

    def File taskDefDir = new File(project.buildDir, "taskDefs")

    EcsUpTask() {
        this.description = "Creates the ECS task definition, service, and starts the containers, if necessary"
    }

    @TaskAction
    def upAction() {
        super.execute { AmazonECSClient ecsClient, Cluster cluster ->
            cluster.containers.all { container ->
                def taskDef = createTaskDefinition(ecsClient, container)
                createService(ecsClient, container, taskDef)
                startTasks(ecsClient, cluster, container, taskDef)
            }
        }
    }

    def createTaskDefinition(AmazonECSClient ecsClient, Container container) {
        logger.quiet("Registering task definition ${container.name}...")

        def family = "${clusterName}-${container.name}"
        def containerDefinition = container.toDefinition()

        def taskDefFile = new File(taskDefDir, "${family}.def")
        taskDefFile.text = containerDefinition.toString()

        def result = ecsClient.registerTaskDefinition(new RegisterTaskDefinitionRequest()
            .withFamily(family)
            .withContainerDefinitions(containerDefinition))

        logger.quiet("${result.taskDefinition.family}:${result.taskDefinition.revision} - ${result.taskDefinition.status}")
        result.taskDefinition
    }

    def createService(AmazonECSClient ecsClient, Container container, TaskDefinition taskDef) {
        logger.quiet("Creating ${container.name} service...")
        def result = ecsClient.createService(new CreateServiceRequest()
            .withCluster(clusterName)
            .withServiceName(container.name)
            .withTaskDefinition(taskDef.getTaskDefinitionArn())
            .withDesiredCount(container.instances))

        logger.quiet("${result.service.serviceArn}:${result.service.status}")
        result.service
    }

    def startTasks(AmazonECSClient ecsClient, Cluster cluster, Container container, TaskDefinition taskDef) {
        logger.quiet("Checking container instances...")
        def containerInstances = listContainerInstances(cluster)
        if(containerInstances.size() < container.instances) {   // TODO: scale up
            throw new GradleException("insufficient number of container instances registered with ${cluster.name} cluster")
        }

        // TODO: use smart algorithm to determine availability
        def containerInstanceArns = containerInstances.collect { instance ->
            instance.containerInstanceArn
        }

        logger.quiet("Starting ${clusterName} tasks...")
        def result = ecsClient.startTask(new StartTaskRequest()
            .withCluster(clusterName)
            .withContainerInstances(containerInstanceArns)
            .withTaskDefinition(taskDef.getTaskDefinitionArn()))

        if(result.failures) {
            throw new GradleException("error starting the task definition: ${result.failures}")
        }
        else {
            logger.quiet("${clusterName}:${result.tasks}")
        }
    }
}
