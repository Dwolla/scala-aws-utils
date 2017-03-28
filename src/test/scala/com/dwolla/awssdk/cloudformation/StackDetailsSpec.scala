package com.dwolla.awssdk.cloudformation

import com.amazonaws.services.cloudformation.model.{CreateChangeSetRequest, CreateStackRequest, Parameter, UpdateStackRequest}
import org.specs2.mutable.Specification

import collection.JavaConverters._
import com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM
import Implicits._

class StackDetailsSpec extends Specification {

  "StackDetails" should {
    "be convertible to CreateStackRequest" in {
      val output: CreateStackRequest = StackDetails("name", "template", List(new Parameter().withParameterKey("key").withParameterValue("value")))

      output must beAnInstanceOf[CreateStackRequest]
      output.getStackName must_== "name"
      output.getTemplateBody must_== "template"
      output.getParameters must_== List(new Parameter().withParameterKey("key").withParameterValue("value")).asJava
      output.getCapabilities must_== List(CAPABILITY_IAM.toString).asJava
      output.getRoleARN must beNull
    }

    "be convertible to UpdateStackRequest" in {
      val output: UpdateStackRequest = StackDetails("name", "template", List(new Parameter().withParameterKey("key").withParameterValue("value")))

      output must beAnInstanceOf[UpdateStackRequest]
      output.getStackName must_== "name"
      output.getTemplateBody must_== "template"
      output.getParameters must_== List(new Parameter().withParameterKey("key").withParameterValue("value")).asJava
      output.getCapabilities must_== List(CAPABILITY_IAM.toString).asJava
      output.getRoleARN must beNull
    }

    "be convertible to CreateChangeSetRequest" in {
      val output: CreateChangeSetRequest = StackDetails("name", "template", List(new Parameter().withParameterKey("key").withParameterValue("value")))

      output must beAnInstanceOf[CreateChangeSetRequest]
      output.getStackName must_== "name"
      output.getTemplateBody must_== "template"
      output.getParameters must_== List(new Parameter().withParameterKey("key").withParameterValue("value")).asJava
      output.getCapabilities must_== List(CAPABILITY_IAM.toString).asJava
      output.getRoleARN must beNull
    }

    "set RoleARN on CreateStackRequest" in {
      val output: CreateStackRequest = StackDetails("name", "template", List.empty[Parameter], roleArn = Option("role-arn"))

      output.getRoleARN must_== "role-arn"
    }

    "set RoleARN on UpdateStackRequest" in {
      val output: UpdateStackRequest = StackDetails("name", "template", List.empty[Parameter], roleArn = Option("role-arn"))

      output.getRoleARN must_== "role-arn"
    }

    "set RoleARN on CreateChangeSetRequest" in {
      val output: CreateChangeSetRequest = StackDetails("name", "template", List.empty[Parameter], roleArn = Option("role-arn"))

      output.getRoleARN must_== "role-arn"
    }
  }

}
