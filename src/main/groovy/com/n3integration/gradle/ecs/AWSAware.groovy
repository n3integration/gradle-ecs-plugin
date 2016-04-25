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
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain
import com.amazonaws.regions.Regions
import com.amazonaws.services.ec2.AmazonEC2Client
import com.amazonaws.services.ec2.model.CreateKeyPairRequest
import com.amazonaws.services.ec2.model.Instance
import com.amazonaws.services.ec2.model.RunInstancesRequest
import com.n3integration.gradle.ecs.models.Cluster

import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.attribute.PosixFilePermission

trait AWSAware {

    def String createKeyPairIfNecessary(Cluster cluster) {
        def privateKey = privateKeyFile(cluster)
        if(!privateKey.exists()) {
            Files.createDirectories(privateKey.getParentFile().toPath())
            Files.createFile(Paths.get(privateKey.getAbsolutePath()))
            Files.setPosixFilePermissions(Paths.get(privateKey.getAbsolutePath()), ownerReadWritePermissions())
        }
        if(privateKey.length() == 0) {
            try {
                def ec2Client = createEc2Client(cluster)
                def createKeyPairResult = ec2Client.createKeyPair(new CreateKeyPairRequest()
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
        return cluster.name
    }

    def List<Instance> createEc2InstancesIfNecessary(Cluster cluster) {
        def ec2Client = createEc2Client(cluster)
        def instanceSettings = cluster.instanceSettings
        def result = ec2Client.runInstances(new RunInstancesRequest()
            .withImageId(instanceSettings.ami)
            .withInstanceType(instanceSettings.type)
            .withMinCount(instanceSettings.min)
            .withMaxCount(instanceSettings.max)
            .withKeyName(cluster.name)
            .withUserData(instanceSettings.userData)
            .withSubnetId(instanceSettings.subnet)
            .withSecurityGroupIds(instanceSettings.securityGroups))

        result.getReservation().instances
    }

    def AmazonEC2Client createEc2Client(Cluster cluster) {
        new AmazonEC2Client(credentials())
            .withRegion(getRegion(cluster))
    }

    def Regions getRegion(Cluster cluster) {
        Regions.fromName(cluster?.region ?: project.ecs.region)
    }

    def AWSCredentials defaultCredentials() {
        def credentialsProvider = new DefaultAWSCredentialsProviderChain()
        credentialsProvider.getCredentials()
    }

    def File privateKeyFile(Cluster cluster) {
        new File(System.getProperty("user.home"), ".ecs/${cluster.name}.pem")
    }

    private AWSCredentials credentials() {
        if(this.class.metaClass.respondsTo(this, "getCredentials")) {
            return getCredentials()
        }
        else {
            return defaultCredentials()
        }
    }

    private Set<PosixFilePermission> ownerReadWritePermissions() {
        def perms = new HashSet<PosixFilePermission>()
        perms.add(PosixFilePermission.OWNER_READ)
        perms.add(PosixFilePermission.OWNER_WRITE)
        perms
    }
}
