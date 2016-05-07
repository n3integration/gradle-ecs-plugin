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
package com.n3integration.gradle.ecs.models

import com.amazonaws.services.autoscaling.model.Tag
import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification
import com.google.common.collect.Lists
import com.google.common.io.BaseEncoding
import groovy.text.SimpleTemplateEngine

import java.nio.charset.Charset

/**
 * Model definition for ec2 instance configuration. Currently supports
 * auto scaling groups.
 *
 * @author n3integration
 */
class Ec2InstanceSettings {

    static final String             DEFAULT_TYPE    = "t2.micro"    // free tier
    static final String             DEFAULT_REGION  = "us-east-1"
    static final Map<String,String> REGION2AMI      =
        ["us-east-1"     : "ami-67a3a90d",
         "us-west-1"     : "ami-b7d5a8d7",
         "us-west-2"     : "ami-c7a451a7",
         "eu-west-1"     : "ami-9c9819ef",
         "eu-central-1"  : "ami-9aeb0af5",
         "ap-northeast-1": "ami-7e4a5b10",
         "ap-southeast-1": "ami-be63a9dd",
         "ap-southeast-2": "ami-b8cbe8db"]

    String name
    String subnet
    String image = REGION2AMI[DEFAULT_REGION]
    String instanceType = DEFAULT_TYPE
    String userData
    String iamInstanceProfileArn
    AutoScaling autoScaling
    List<String> securityGroups
    List<Tag> tags

    Ec2InstanceSettings(String name) {
        this.name = name
        this.tags = Lists.newArrayList()
        this.userData = loadUserDataTemplate(name)
    }

    def IamInstanceProfileSpecification instanceProfileSpecification() {
        if(iamInstanceProfileArn) {
            return new IamInstanceProfileSpecification()
                .withArn(iamInstanceProfileArn)
        }
        return null
    }

    def tag(@DelegatesTo(Tag) Closure closure) {
        def tag = new Tag()
        def clone = closure.rehydrate(tag, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
        this.tags.add(tag)
    }

    def autoScaling(@DelegatesTo(AutoScaling) Closure closure) {
        this.autoScaling = new AutoScaling()
        def clone = closure.rehydrate(autoScaling, this, this)
        clone.resolveStrategy = Closure.DELEGATE_ONLY
        clone()
    }

    static String loadUserDataTemplate(String name) {
        def binding = [name:name, proxy:proxy()]
        def engine = new SimpleTemplateEngine()
        def template = engine.createTemplate(loadDefaultUserData())
            .make(binding)
        BaseEncoding.base64().encode(template.toString().getBytes(Charset.defaultCharset()))
    }

    static String proxy() {
        return System.getenv("HTTP_PROXY") ? System.getenv("HTTP_PROXY")
                : System.getProperty("http.proxy")
    }

    private static String loadDefaultUserData() {
        Ec2InstanceSettings.class.getResource("/userdata.sh").text
    }
}
