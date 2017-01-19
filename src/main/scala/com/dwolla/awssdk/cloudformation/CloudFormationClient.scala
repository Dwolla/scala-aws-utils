package com.dwolla.awssdk.cloudformation

import java.io.Closeable

import com.amazonaws.regions.Regions
import com.amazonaws.regions.Regions.US_WEST_2
import com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM
import com.amazonaws.services.cloudformation.model.StackStatus._
import com.amazonaws.services.cloudformation.model.{Parameter ⇒ AwsParameter, _}
import com.amazonaws.services.cloudformation.{AmazonCloudFormationAsync, AmazonCloudFormationAsyncClient}
import com.dwolla.awssdk.cloudformation.CloudFormationClient.{StackID, updatableStackStatuses}
import com.dwolla.awssdk.cloudformation.Implicits._
import com.dwolla.awssdk.utils.ScalaAsyncHandler.Implicits._

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, reflectiveCalls}

trait CloudFormationClient {
  def createOrUpdateTemplate(stackName: String, template: String, params: List[(String, String)] = List.empty[(String, String)], roleArn: Option[String] = None): Future[StackID]
}

class CloudFormationClientImpl(client: AmazonCloudFormationAsync)(implicit ec: ExecutionContext) extends CloudFormationClient with AutoCloseable with Closeable {

  override def createOrUpdateTemplate(stackName: String,
                                      template: String,
                                      params: List[(String, String)] = List.empty[(String, String)],
                                      roleArn: Option[String] = None): Future[StackID] = {
    val requestBuilder = StackDetails(stackName, template, params, roleArn)

    getStackByName(stackName).flatMap(
      _.fold(createStack(requestBuilder)) {
        case stack if updatableStackStatuses.contains(stackStatus(stack.getStackStatus)) ⇒ updateStack(requestBuilder)
        case stack ⇒ throw StackNotUpdatableException(stack.getStackName, stack.getStackStatus)
      }
    )
  }

  private def getStackByName(name: String): Future[Option[Stack]] = new DescribeStacksRequest().to[DescribeStacksResult].via(client.describeStacksAsync)
    .map(_.getStacks.asScala.filter(s ⇒ s.getStackName == name && StackStatus.valueOf(s.getStackStatus) != DELETE_COMPLETE).toList.headOption)

  private def createStack(createStackRequest: CreateStackRequest): Future[StackID] = extractStackIdFrom(createStackRequest.to[CreateStackResult].via(client.createStackAsync))

  private def updateStack(updateStackRequest: UpdateStackRequest): Future[StackID] = extractStackIdFrom(updateStackRequest.to[UpdateStackResult].via(client.updateStackAsync))

  //noinspection AccessorLikeMethodIsEmptyParen
  private def extractStackIdFrom[AwsResult <: {def getStackId(): StackID}](eventualResult: Future[AwsResult]): Future[StackID] = eventualResult.map(_.getStackId())

  override def close(): Unit = client.shutdown()
}

object CloudFormationClient {
  import concurrent.ExecutionContext.Implicits.global
  type StackID = String

  def apply(): CloudFormationClient = apply(US_WEST_2)

  def apply(r: String): CloudFormationClient = apply(Regions.fromName(r))

  def apply(r: Regions): CloudFormationClient = new CloudFormationClientImpl(clientForRegion(r))

  val updatableStackStatuses = Seq(
    CREATE_COMPLETE,
    ROLLBACK_COMPLETE,
    UPDATE_COMPLETE,
    UPDATE_ROLLBACK_COMPLETE
  )

  private def clientForRegion(r: Regions) = {
    val x = new AmazonCloudFormationAsyncClient()
    x.configureRegion(r)
    x
  }
}

case class StackDetails(name: String, template: String, parameters: List[AwsParameter], roleArn: Option[String] = None)

trait Builder[StackRequest] {
  def withStackName(name: String): StackRequest
  def withTemplateBody(name: String): StackRequest
  def withParameters(params: List[AwsParameter]): StackRequest
  def withCapabilities(capabilities: Capability*): StackRequest
  def withRoleArn(roleArn: String): StackRequest
}

object Implicits {
  implicit class CreateStackRequestToBuilder(s: CreateStackRequest) extends Builder[CreateStackRequest] {
    override def withStackName(name: String): CreateStackRequest = s.withStackName(name)
    override def withTemplateBody(name: String): CreateStackRequest = s.withTemplateBody(name)
    override def withParameters(params: List[AwsParameter]): CreateStackRequest = s.withParameters(params.asJavaCollection)
    override def withCapabilities(capabilities: Capability*): CreateStackRequest = s.withCapabilities(capabilities: _*)
    override def withRoleArn(roleArn: String): CreateStackRequest = s.withRoleARN(roleArn)
  }
  implicit class UpdateStackRequestToBuilder(s: UpdateStackRequest) extends Builder[UpdateStackRequest] {
    override def withStackName(name: String): UpdateStackRequest = s.withStackName(name)
    override def withTemplateBody(name: String): UpdateStackRequest = s.withTemplateBody(name)
    override def withParameters(params: List[AwsParameter]): UpdateStackRequest = s.withParameters(params.asJavaCollection)
    override def withCapabilities(capabilities: Capability*): UpdateStackRequest = s.withCapabilities(capabilities: _*)
    override def withRoleArn(roleArn: String): UpdateStackRequest = s.withRoleARN(roleArn)
  }

  implicit def potentialStackToCreateRequest(ps: StackDetails): CreateStackRequest = populate(ps, new CreateStackRequest)
  implicit def potentialStackToUpdateRequest(ps: StackDetails): UpdateStackRequest = populate(ps, new UpdateStackRequest)

  implicit def tuplesToParams(tuples: List[(String, String)]): List[AwsParameter] = tuples.map {
    case (key, value) ⇒ new AwsParameter().withParameterKey(key).withParameterValue(value)
  }
  implicit def stackStatus(status: String): StackStatus = StackStatus.valueOf(status)

  private def populate[T](ps:StackDetails, builder: Builder[T])(implicit ev: T ⇒ Builder[T]): T = {
    val t = builder.withStackName(ps.name)
      .withTemplateBody(ps.template)
      .withParameters(ps.parameters)
      .withCapabilities(CAPABILITY_IAM)

    ps.roleArn.fold(t)(t.withRoleArn)
  }
}
