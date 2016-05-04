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

/**
 *
 */
trait ECSAware extends AWSAware {

    def AmazonECSClient createEcsClient(cluster) {
        new AmazonECSClient(credentials())
            .withRegion(getRegion(cluster))
    }

    def CreateClusterResult createCluster(AmazonECSClient client, Cluster cluster) {
        client.createCluster(new CreateClusterRequest()
            .withClusterName(cluster.name))
    }

    def void scaleServices(AmazonECSClient client, Cluster cluster) {
        client.listServices(new ListServicesRequest()
                .withCluster(cluster.name)).serviceArns.each { service ->

            client.updateService(new UpdateServiceRequest()
                    .withCluster(cluster.name)
                    .withService(service)
                    .withDesiredCount(cluster.instanceSettings.autoScaling.min))
        }
    }

    def void deleteServices(AmazonECSClient client, Cluster cluster) {
        def services = client.listServices(new ListServicesRequest()
            .withCluster(cluster.name))

        services.serviceArns.each { service ->
            client.deleteService(new DeleteServiceRequest()
                .withCluster(cluster.name)
                .withService(service))
        }
    }

    def DeleteClusterResult deleteCluster(AmazonECSClient client, Cluster cluster) {
        client.deleteCluster(new DeleteClusterRequest()
            .withCluster(cluster.name))
    }

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

    def List<ContainerInstance> waitForContainerInstances(AmazonECSClient client, Cluster cluster, long timeout) {
        def instances = listContainerInstances(client, cluster)
        while(instances.isEmpty()) {
            sleep(timeout)
            instances = listContainerInstances(client, cluster)
        }
        instances
    }
}