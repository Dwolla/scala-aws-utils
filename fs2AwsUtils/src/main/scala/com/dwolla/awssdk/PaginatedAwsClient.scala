package com.dwolla.awssdk

import cats.effect.Effect
import cats.implicits._
import com.dwolla.awssdk.ExecuteVia._
import com.dwolla.awssdk.utils._
import com.dwolla.fs2.Pagination
import fs2._

import scala.language.{higherKinds, reflectiveCalls}

object PaginatedAwsClient {

  class PaginatedAwsClient[F[_] : Effect, Req <: PaginatedRequest, Res <: PaginatedResult, T](requestFactory: () ⇒ Req) {
    def via(awsAsyncFunction: AwsAsyncFunction[Req, Res])(extractor: Res ⇒ Seq[T]): Stream[F, T] = {
      val fetchPage = (maybeNextToken: Option[String]) ⇒ {
        val req = requestFactory()
        maybeNextToken.foreach(req.setNextToken)

        req.executeVia[F](awsAsyncFunction).map((res: Res) ⇒ (Segment.seq(extractor(res)), Option(res.getNextToken())))
      }

      Pagination.offsetUnfoldSegmentEval(fetchPage)
    }
  }

  implicit class FetchAll[Req <: PaginatedRequest](val requestFactory: () ⇒ Req) {
    def fetchAll[F[_]] = new PartiallyApplied[F]
    final class PartiallyApplied[F[_]] {
      def apply[Res <: PaginatedResult, T](awsAsyncFunction: AwsAsyncFunction[Req, Res])
                                          (extractor: Res ⇒ Seq[T])
                                          (implicit F: Effect[F]): Stream[F, T] =
        new PaginatedAwsClient(requestFactory).via(awsAsyncFunction)(extractor)
    }
  }

}
