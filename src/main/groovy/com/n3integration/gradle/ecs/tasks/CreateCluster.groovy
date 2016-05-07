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

import com.n3integration.gradle.ecs.AutoScaleAware
import com.n3integration.gradle.ecs.Ec2Aware
import org.gradle.api.tasks.TaskAction

class CreateCluster extends DefaultClusterTask implements Ec2Aware, AutoScaleAware {

    CreateCluster() {
        this.description = "Creates a new EC2 Container Service cluster"
    }

    @TaskAction
    def createClusterAction() {
        def asClient = createAutoScalingClient()
        super.execute { ecsClient, cluster ->
            logger.quiet("Creating ${clusterName} cluster...")
            def result = createCluster(ecsClient, cluster)
            logger.quiet("\tcluster: ${clusterName}")
            logger.quiet("\t status: ${result.cluster?.status}")

            def ec2Client = createEc2Client(cluster)
            logger.quiet("Creating key pair...")
            result = createKeyPairIfNecessary(ec2Client, cluster)
            logger.quiet("\tkey: ${result}")

            def instanceSettings = cluster.instanceSettings
            if(instanceSettings && instanceSettings.autoScaling) {
                logger.quiet("Creating launch configuration...")
                result = createLaunchConfiguration(asClient, instanceSettings)
                logger.quiet("\tcreated config: ${result}")
                logger.quiet("Creating auto scaling group...")
                result = createAutoScalingGroup(asClient, instanceSettings)
                logger.quiet("\tcreated group: ${result}")
            }
        }
    }
}
