package com.dwolla.awssdk.cloudformation

import java.io.Closeable
import java.util.concurrent.{Future ⇒ JFuture}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.regions.Regions
import com.amazonaws.regions.Regions.US_WEST_2
import com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM
import com.amazonaws.services.cloudformation.model.StackStatus._
import com.amazonaws.services.cloudformation.model.{Parameter ⇒ AwsParameter, _}
import com.amazonaws.services.cloudformation.{AmazonCloudFormationAsync, AmazonCloudFormationAsyncClient}
import com.dwolla.awssdk.cloudformation.CloudFormationClient.{StackID, updatableStackStatuses}
import com.dwolla.awssdk.cloudformation.Implicits._
import com.dwolla.awssdk.utils.ScalaAsyncHandler

import scala.collection.JavaConversions._
import scala.concurrent.{ExecutionContext, Future}
import scala.language.{implicitConversions, reflectiveCalls}

trait CloudFormationClient {
  def createOrUpdateTemplate(stackName: String, template: String, params: List[(String, String)] = List.empty[(String, String)]): Future[StackID]
}

class CloudFormationClientImpl(client: AmazonCloudFormationAsync)(implicit ec: ExecutionContext) extends CloudFormationClient with AutoCloseable with Closeable {

  override def createOrUpdateTemplate(stackName: String, template: String, params: List[(String, String)] = List.empty[(String, String)]): Future[StackID] = {
    def getStackByName(name: String) = withHandler[DescribeStacksRequest, DescribeStacksResult](client.describeStacksAsync)
      .map(_.getStacks.filter(s ⇒ s.getStackName == name && StackStatus.valueOf(s.getStackStatus) != DELETE_COMPLETE).toList.headOption)

    def createStack(createStackRequest: CreateStackRequest) = extractStackId[CreateStackRequest, CreateStackResult](client.createStackAsync(createStackRequest, _))

    def updateStack(updateStackRequest: UpdateStackRequest) = extractStackId[UpdateStackRequest, UpdateStackResult](client.updateStackAsync(updateStackRequest, _))

    //noinspection AccessorLikeMethodIsEmptyParen
    def extractStackId[A <: AmazonWebServiceRequest, B <: {def getStackId(): String}](f: (ScalaAsyncHandler[A, B]) ⇒ JFuture[B]) = withHandler[A, B](f).map(_.getStackId())

    def withHandler[A <: AmazonWebServiceRequest, B](f: (ScalaAsyncHandler[A, B]) ⇒ JFuture[B]) = {
      val handler = new ScalaAsyncHandler[A, B]
      f(handler)
      handler.future
    }

    val requestBuilder = StackDetails(stackName, template, params)

    getStackByName(stackName).flatMap(
      _.fold(createStack(requestBuilder)) {
        case stack if updatableStackStatuses.contains(stackStatus(stack.getStackStatus)) ⇒ updateStack(requestBuilder)
        case stack ⇒ throw StackNotUpdatableException(stack.getStackName, stack.getStackStatus)
      }
    )
  }

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

case class StackDetails(name: String, template: String, parameters: List[AwsParameter])

trait Builder[T] {
  def withStackName(name: String): T
  def withTemplateBody(name: String): T
  def withParameters(params: List[AwsParameter]): T
  def withCapabilities(capabilities: Capability*): T
}

object Implicits {
  implicit class CreateStackRequestToBuilder(s: CreateStackRequest) extends Builder[CreateStackRequest] {
    override def withStackName(name: String): CreateStackRequest = s.withStackName(name)
    override def withTemplateBody(name: String): CreateStackRequest = s.withTemplateBody(name)
    override def withParameters(params: List[AwsParameter]): CreateStackRequest = s.withParameters(params)
    override def withCapabilities(capabilities: Capability*): CreateStackRequest = s.withCapabilities(capabilities: _*)
  }
  implicit class UpdateStackRequestToBuilder(s: UpdateStackRequest) extends Builder[UpdateStackRequest] {
    override def withStackName(name: String) = s.withStackName(name)
    override def withTemplateBody(name: String) = s.withTemplateBody(name)
    override def withParameters(params: List[AwsParameter]) = s.withParameters(params)
    override def withCapabilities(capabilities: Capability*): UpdateStackRequest = s.withCapabilities(capabilities: _*)
  }

  implicit def potentialStackToCreateRequest(ps: StackDetails): CreateStackRequest = populate(ps, new CreateStackRequest)
  implicit def potentialStackToUpdateRequest(ps: StackDetails): UpdateStackRequest = populate(ps, new UpdateStackRequest)

  implicit def tuplesToParams(tuples: List[(String, String)]): List[AwsParameter] = tuples.map {
    case (key, value) ⇒ new AwsParameter().withParameterKey(key).withParameterValue(value)
  }
  implicit def stackStatus(status: String): StackStatus = StackStatus.valueOf(status)

  private def populate[T](ps:StackDetails, builder: Builder[T])(implicit ev: T ⇒ Builder[T]): T = {
    builder.withStackName(ps.name)
      .withTemplateBody(ps.template)
      .withParameters(ps.parameters)
      .withCapabilities(CAPABILITY_IAM)
  }
}
