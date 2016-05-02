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

import com.amazonaws.services.autoscaling.AmazonAutoScalingClient
import com.amazonaws.services.autoscaling.model.CreateAutoScalingGroupRequest
import com.amazonaws.services.autoscaling.model.CreateLaunchConfigurationRequest
import com.n3integration.gradle.ecs.models.Ec2InstanceSettings

trait AutoScaleAware extends AWSAware {

    static final String ASG_SUFFIX = "asg-cfg"
    static final String LC_SUFFIX  = "launch-cfg"

    /**
     * Initializes a new auto scaling client
     *
     * @return a new {@link AmazonAutoScalingClient} instance
     */
    def AmazonAutoScalingClient createAutoScalingClient() {
        new AmazonAutoScalingClient(credentials())
    }

    /**
     * Creates a new ec2 launch configuration
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     * @return the launch configuration name
     */
    def String createLaunchConfiguration(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        def cfgName = "${settings.name}-${LC_SUFFIX}"
        client.createLaunchConfiguration(new CreateLaunchConfigurationRequest()
            .withImageId(settings.image)
            .withInstanceType(settings.instanceType)
            .withKeyName(settings.name)
            .withIamInstanceProfile(settings.iamInstanceProfileArn)
            .withSecurityGroups(settings.securityGroups)
            .withLaunchConfigurationName(cfgName)
            .withUserData(settings.userData))
        cfgName
    }

    /**
     * Creates a new ec2 auto scaling group
     *
     * @param client
     *          the {@link AmazonAutoScalingClient}
     * @param settings
     *          the {@link Ec2InstanceSettings}
     * @return the auto scaling group name
     */
    def String createAutoScalingGroup(AmazonAutoScalingClient client, Ec2InstanceSettings settings) {
        def cfgName = "${settings.name}-${ASG_SUFFIX}"
        def autoScaling = settings.autoScaling
        client.createAutoScalingGroup(new CreateAutoScalingGroupRequest()
            .withAutoScalingGroupName(cfgName)
            .withVPCZoneIdentifier(autoScaling.subnetIds.join(","))
            .withAvailabilityZones(autoScaling.availabilityZones)
            .withLaunchConfigurationName("${settings.name}-${LC_SUFFIX}")
            .withMinSize(autoScaling.min)
            .withMaxSize(autoScaling.max)
            .withDesiredCapacity(autoScaling.min)
            .withDefaultCooldown(autoScaling.cooldownPeriod)
            .withHealthCheckType(autoScaling.healthCheck?.type)
            .withHealthCheckGracePeriod(autoScaling.healthCheck?.gracePeriod)
            .withTags(settings.tags))
        cfgName
    }
}