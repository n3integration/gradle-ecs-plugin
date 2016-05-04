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

import com.amazonaws.AmazonServiceException
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateKeyPairRequest
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesRequest
import com.amazonaws.services.ec2.model.TerminateInstancesResult
import com.n3integration.gradle.ecs.models.Cluster

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

trait Ec2Aware extends AWSAware {

    def AmazonEC2Client createEc2Client(Cluster cluster) {
        new AmazonEC2Client(credentials())
            .withRegion(getRegion(cluster))
    }

    def String createKeyPairIfNecessary(AmazonEC2Client client, Cluster cluster) {
        def privateKey = privateKeyFile(cluster)

        if(!privateKey.exists()) {
            Files.createDirectories(privateKey.getParentFile().toPath())
            Files.createFile(Paths.get(privateKey.getAbsolutePath()))

            def permissions = ownerReadWritePermissions()
            Files.setPosixFilePermissions(Paths.get(privateKey.getAbsolutePath()), permissions)
        }

        if(privateKey.length() == 0) {
            try {
                def createKeyPairResult = client.createKeyPair(new CreateKeyPairRequest()
                    .withKeyName(cluster.name))

                def keyPair = createKeyPairResult.getKeyPair()
                privateKey.text = keyPair.getKeyMaterial()
            }
            catch(AmazonServiceException e) {
                if(!e.errorCode.equals("InvalidKeyPair.Duplicate")) {
                    throw e;
                }
            }
        }
        return privateKey.absolutePath
    }

    def List<Instance> createEc2Instances(AmazonEC2Client client, Cluster cluster) {
        def instanceSettings = cluster.instanceSettings
        def scale = instanceSettings.scale
        def result = client.runInstances(new RunInstancesRequest()
            .withImageId(instanceSettings.image)
            .withInstanceType(instanceSettings.instanceType)
            .withMinCount(scale.min)
            .withMaxCount(scale.max)
            .withKeyName(cluster.name)
            .withUserData(instanceSettings.userData)
            .withSubnetId(instanceSettings.subnet)
            .withIamInstanceProfile(instanceSettings.instanceProfileSpecification())
            .withSecurityGroupIds(instanceSettings.securityGroups))

        result.getReservation().instances
    }

    def TerminateInstancesResult deleteEc2Instances(AmazonEC2Client client, List<String> instanceIds) {
        client.terminateInstances(new TerminateInstancesRequest()
                .withInstanceIds(instanceIds))
    }

    def File privateKeyFile(Cluster cluster) {
        new File(System.getProperty("user.home"), ".ecs/${cluster.name}.pem")
    }

    private Set<PosixFilePermission> ownerReadWritePermissions() {
        def perms = new HashSet<PosixFilePermission>()
        perms.add(PosixFilePermission.OWNER_READ)
        perms.add(PosixFilePermission.OWNER_WRITE)
        perms
    }
}