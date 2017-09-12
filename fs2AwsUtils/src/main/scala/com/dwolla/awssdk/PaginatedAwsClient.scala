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
      def fetchPage(maybeNextToken: Option[String]): F[(Segment[T, Unit], Option[String])] = {
        val req = requestFactory()
        maybeNextToken.foreach(req.setNextToken)

        req.executeVia[F](awsAsyncFunction).map((res: Res) ⇒ (Segment.seq(extractor(res)), Option(res.getNextToken())))
      }

      Pagination.streamContainingAllPages(fetchPage)
    }
  }

  implicit class FetchAll[Req <: PaginatedRequest](val requestFactory: () ⇒ Req) {
    class EffectBinder[F[_]] {
      def apply[Res <: PaginatedResult, T](awsAsyncFunction: AwsAsyncFunction[Req, Res])
                                          (extractor: Res ⇒ Seq[T])
                                          (implicit F: Effect[F]): Stream[F, T] =
        new PaginatedAwsClient[F, Req, Res, T](requestFactory).via(awsAsyncFunction)(extractor)
    }

    def fetchAll[F[_]] = new EffectBinder[F]
  }

}
