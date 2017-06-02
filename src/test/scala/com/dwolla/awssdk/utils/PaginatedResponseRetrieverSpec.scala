package com.dwolla.awssdk.utils

import com.amazonaws.services.ecs.AmazonECSAsync
import com.amazonaws.services.ecs.model.{ListClustersRequest, ListClustersResult, ListContainerInstancesRequest, ListContainerInstancesResult}
import com.dwolla.awssdk.AmazonAsyncMockingImplicits._
import com.dwolla.awssdk.utils.PaginatedResponseRetriever._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.collection.JavaConverters._

class PaginatedResponseRetrieverSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    val mockEcsClient = mock[AmazonECSAsync]
  }

  "PaginatedResponseRetriever" should {
    "make all the requests necessary to fetch all paginated results" in new Setup {
      def reqWithNextToken(x: Option[Int]) = new ListContainerInstancesRequest().withCluster("cluster1").withNextToken(x.map(i ⇒ s"next-token-$i").orNull)

      def res(x: Int, y: Option[Int] = None) = Right(new ListContainerInstancesResult().withContainerInstanceArns(s"arn$x").withNextToken(y.map(i ⇒ s"next-token-$i").orNull))

      val pages = 1 to 50
      val pairs = pages.sliding(2).toSeq.map {
        case Vector(1, y) ⇒ reqWithNextToken(None) → res(1, Option(y))
        case Vector(x, y) if x > 1 && y < pages.last ⇒ reqWithNextToken(Option(x)) → res(x, Option(y))
        case Vector(x, _) ⇒ reqWithNextToken(Option(x)) → res(x, None)
      }

      mockedMethod(mockEcsClient.listContainerInstancesAsync) answers (pairs: _*)

      val output = fetchAll(() ⇒ new ListContainerInstancesRequest().withCluster("cluster1"),mockEcsClient.listContainerInstancesAsync)
        .map(_.flatMap(_.getContainerInstanceArns.asScala.toList))

      output must containTheSameElementsAs(pages.dropRight(1).map(x ⇒ s"arn$x")).await
    }

    "support default request factory" in new Setup {
      new ListClustersResult() completes mockEcsClient.listClustersAsync

      val output = fetchAllWithDefaultRequestsVia(mockEcsClient.listClustersAsync)

      output must contain(new ListClustersResult()).await
    }

    "support builder syntax with factory as initial parameter" in new Setup {
      new ListClustersResult() completes mockEcsClient.listClustersAsync

      val output = fetchAllWithRequestsLike(() ⇒ new ListClustersRequest).via(mockEcsClient.listClustersAsync)

      output must contain(new ListClustersResult()).await
    }
  }
}
