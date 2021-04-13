package com.dwolla.awssdk.cloudformation

import java.io.Closeable

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.regions.Regions
import com.amazonaws.regions.Regions.US_WEST_2
import com.amazonaws.services.cloudformation._
import com.amazonaws.services.cloudformation.model.Capability.CAPABILITY_IAM
import com.amazonaws.services.cloudformation.model.ChangeSetType._
import com.amazonaws.services.cloudformation.model.StackStatus._
import com.amazonaws.services.cloudformation.model.{Parameter => AwsParameter, _}
import com.dwolla.awssdk.cloudformation.CloudFormationClient._
import com.dwolla.awssdk.cloudformation.Implicits._
import com.dwolla.awssdk.utils.ScalaAsyncHandler.Implicits._
import com.dwolla.awssdk.utils._

import scala.jdk.CollectionConverters._
import scala.concurrent._

trait CloudFormationClient {
  def createOrUpdateTemplate(stackName: String,
                             template: String,
                             params: List[(String, String)] = List.empty[(String, String)],
                             roleArn: Option[String] = None,
                             changeSetName: Option[String] = None): Future[StackID]
}

class CloudFormationClientImpl(client: AmazonCloudFormationAsync)(implicit ec: ExecutionContext) extends CloudFormationClient with AutoCloseable with Closeable {

  override def createOrUpdateTemplate(stackName: String,
                                      template: String,
                                      params: List[(String, String)] = List.empty[(String, String)],
                                      roleArn: Option[String] = None,
                                      changeSetName: Option[String] = None): Future[StackID] = {
    val requestBuilder = StackDetails(stackName, template, params, roleArn)

    getStackByName(stackName).flatMap { maybeStack =>
      val stackOperation = maybeStack.fold(buildStackOperation(createStack, CREATE)) {
        case stack if updatableStackStatuses.contains(stackStatus(stack.getStackStatus)) => buildStackOperation(updateStack, UPDATE)
        case stack => throw StackNotUpdatableException(stack.getStackName, stack.getStackStatus)
      }

      stackOperation(requestBuilder, changeSetName)
    }
  }

  private def buildStackOperation[T](func: T => Future[StackID], changeSetType: ChangeSetType)
                                    (implicit ev1: StackDetails => T): (StackDetails, Option[String]) => Future[StackID] =
    (stackDetails: StackDetails, changeSetName: Option[String]) => changeSetName.fold(func(stackDetails))(createChangeSet(_, stackDetails.withChangeSetType(changeSetType)))

  private def getStackByName(name: String): Future[Option[Stack]] =
    for {
      results <- PaginatedResponseRetriever.fetchAll(() => new DescribeStacksRequest(), client.describeStacksAsync)
      stacks = results.flatMap(_.getStacks.asScala)
    } yield stacks.find(s => s.getStackName == name && StackStatus.valueOf(s.getStackStatus) != DELETE_COMPLETE)

  private def createStack(createStackRequest: CreateStackRequest): Future[StackID] = makeRequestAndExtractStackId(createStackRequest, client.createStackAsync)

  private def updateStack(updateStackRequest: UpdateStackRequest): Future[StackID] = makeRequestAndExtractStackId(updateStackRequest, client.updateStackAsync)

  private def createChangeSet(changeSetName: String, createChangeSetRequest: CreateChangeSetRequest): Future[StackID] = makeRequestAndExtractStackId(createChangeSetRequest.withChangeSetName(changeSetName), client.createChangeSetAsync)

  //noinspection AccessorLikeMethodIsEmptyParen
  private def makeRequestAndExtractStackId[Req <: AmazonWebServiceRequest, Res <: {def getStackId(): StackID}](req: Req, func: AwsAsyncFunction[Req, Res]): Future[StackID] =
    req.via(func).map(_.getStackId())

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

  private def clientForRegion(r: Regions) = AmazonCloudFormationAsyncClientBuilder.standard().withRegion(r).build()
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
  implicit class CreateChangeSetRequestToBuilder(s: CreateChangeSetRequest) extends Builder[CreateChangeSetRequest] {
    override def withStackName(name: String): CreateChangeSetRequest = s.withStackName(name)
    override def withTemplateBody(name: String): CreateChangeSetRequest = s.withTemplateBody(name)
    override def withParameters(params: List[AwsParameter]): CreateChangeSetRequest = s.withParameters(params.asJavaCollection)
    override def withCapabilities(capabilities: Capability*): CreateChangeSetRequest = s.withCapabilities(capabilities: _*)
    override def withRoleArn(roleArn: String): CreateChangeSetRequest = s.withRoleARN(roleArn)
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
  implicit def potentialStackToCreateChangeSetRequest(ps: StackDetails): CreateChangeSetRequest = populate(ps, new CreateChangeSetRequest)

  implicit def tuplesToParams(tuples: List[(String, String)]): List[AwsParameter] = tuples.map {
    case (key, value) => new AwsParameter().withParameterKey(key).withParameterValue(value)
  }
  implicit def stackStatus(status: String): StackStatus = StackStatus.valueOf(status)

  private def populate[T](ps:StackDetails, builder: Builder[T])(implicit ev: T => Builder[T]): T = {
    val t = builder.withStackName(ps.name)
      .withTemplateBody(ps.template)
      .withParameters(ps.parameters)
      .withCapabilities(CAPABILITY_IAM)

    ps.roleArn.fold(t)(t.withRoleArn)
  }
}
