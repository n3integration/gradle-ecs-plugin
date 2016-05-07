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

import com.amazonaws.services.ecs.AmazonECSClient
import com.amazonaws.services.ecs.model.*
import com.google.common.collect.Lists
import com.n3integration.gradle.ecs.models.Cluster
import org.gradle.api.GradleException

/**
 * Collection of ECS methods
 *
 * @author n3integration
 */
trait ECSAware extends AWSAware {

    def File taskDefDir = new File(project.buildDir, "taskDefs")

    /**
     * Instantiates a new {@link AmazonECSClient} instance
     *
     * @param cluster
     *          the {@link Cluster} definition
     * @return a new {@link AmazonECSClient}
     */
    def AmazonECSClient createEcsClient(Cluster cluster) {
        new AmazonECSClient(credentials())
            .withRegion(getRegion(cluster))
    }

    /**
     * Creates a new ECS cluster
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @return
     */
    def CreateClusterResult createCluster(AmazonECSClient client, Cluster cluster) {
        client.createCluster(new CreateClusterRequest()
            .withClusterName(cluster.name))
    }

    /**
     * Scales the number of running services up or down according to minimum
     * number of auto scaling instances defined for the {@code cluster}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     */
    def void scaleServices(AmazonECSClient client, Cluster cluster) {
        client.listServices(new ListServicesRequest()
            .withCluster(cluster.name)).serviceArns.each { service ->

            client.updateService(new UpdateServiceRequest()
                .withCluster(cluster.name)
                .withService(service)
                .withDesiredCount(cluster.instanceSettings.autoScaling.min))
        }
    }

    /**
     * Deletes all {@code cluster} services
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     */
    def void deleteServices(AmazonECSClient client, Cluster cluster) {
        def services = client.listServices(new ListServicesRequest()
            .withCluster(cluster.name))

        services.serviceArns.each { service ->
            client.deleteService(new DeleteServiceRequest()
                .withCluster(cluster.name)
                .withService(service))
        }
    }

    /**
     * Deletes the provided {@code cluster}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @return {@link DeleteClusterResult}
     */
    def DeleteClusterResult deleteCluster(AmazonECSClient client, Cluster cluster) {
        client.deleteCluster(new DeleteClusterRequest()
            .withCluster(cluster.name))
    }

    /**
     * Retrieves the list of ec2 instances that are registered with the provided {@code cluster}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @return the {@link List} of {@link ContainerInstance}s registered with the cluster
     */
    def List<ContainerInstance> listContainerInstances(AmazonECSClient client, Cluster cluster) {
        def result = client.listContainerInstances(new ListContainerInstancesRequest()
            .withCluster(cluster.name))

        if(result.containerInstanceArns.isEmpty()) {
            return Lists.newArrayList()
        }

        def instanceResult = client.describeContainerInstances(new DescribeContainerInstancesRequest()
            .withCluster(cluster.name)
            .withContainerInstances(result.containerInstanceArns))

        instanceResult.containerInstances.findAll { instance ->
            instance.getStatus().equals("ACTIVE")
        }
    }

    /**
     * Utility method to allow for ec2 instances to come online
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @param timeout
     *          the number of milliseconds to pause wh
     * @return the {@link List} of available {@link ContainerInstance}s
     */
    def List<ContainerInstance> waitForContainerInstances(AmazonECSClient client, Cluster cluster, long timeout) {
        def instances = listContainerInstances(client, cluster)
        while(instances.isEmpty()) {
            sleep(timeout)
            instances = listContainerInstances(client, cluster)
        }
        instances
    }

    /**
     * Creates a {@link TaskDefinition} from the provided {@code container}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @param familyName
     *          the task family name
     * @param containers
     *          the {@link List} of {@link Container} definitions
     * @return the {@link TaskDefinition}
     */
    def TaskDefinition createTaskDefinition(AmazonECSClient client, Cluster cluster, String familyName, List<com.n3integration.gradle.ecs.models.Container> containers) {
        def family = "${cluster.name}-${familyName}"
        def containerDefinitions = containers.collect { container ->
            def containerDef = container.toDefinition()
            def taskDefFile = new File(taskDefDir, "${family}.def")
            taskDefFile.text = containerDef.toString()
            containerDef
        }

        def result = client.registerTaskDefinition(new RegisterTaskDefinitionRequest()
            .withFamily(family)
            .withContainerDefinitions(containerDefinitions))

        result.taskDefinition
    }

    /**
     * Unregisters all tasks associated with {@code family}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param family
     *          the task definition family name
     */
    def void unregisterTasks(AmazonECSClient client, String family) {
        def result = client.listTaskDefinitions(new ListTaskDefinitionsRequest()
                .withFamilyPrefix(family))

        result.taskDefinitionArns.each { arn ->
            client.deregisterTaskDefinition(new DeregisterTaskDefinitionRequest()
                .withTaskDefinition(arn))
        }
    }

    /**
     * Creates an ECS service using the provided {@code container}
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @param name
     *          the service name
     * @param count
     *          the desired service count
     * @param taskDef
     *          the {@link TaskDefinition}
     * @return the {@link Service}
     */
    def Service createService(AmazonECSClient client, Cluster cluster, String name, int count, TaskDefinition taskDef) {
        def result = client.createService(new CreateServiceRequest()
            .withCluster(cluster.name)
            .withClientToken(UUID.randomUUID().toString())
            .withServiceName(name)
            .withTaskDefinition(taskDef.getTaskDefinitionArn())
            .withDesiredCount(count))

        result.service
    }

    /**
     * Starts the ECS service using the registered ec2 containers
     *
     * @param client
     *          the {@link AmazonECSClient} instance
     * @param cluster
     *          the {@link Cluster} definition
     * @param count
     *          the number of instances
     * @param taskDef
     * @return the {@link List} of started {@link Task}s
     */
    def List<Task> startTasks(AmazonECSClient client, Cluster cluster, int count, TaskDefinition taskDef) {
        def containerInstances = waitForContainerInstances(client, cluster, 10000)
        if(containerInstances.size() < count) {
            throw new GradleException("insufficient number of container instances registered with ${cluster.name} cluster")
        }

        // TODO: use smart algorithm to determine availability
        def containerInstanceArns = containerInstances.collect { instance ->
            instance.containerInstanceArn
        }

        def result = client.startTask(new StartTaskRequest()
            .withCluster(cluster.name)
            .withContainerInstances(containerInstanceArns)
            .withTaskDefinition(taskDef.getTaskDefinitionArn()))

        if(result.failures) {
            throw new GradleException("error starting the task definition: ${result.failures}")
        }

        result.tasks
    }
}