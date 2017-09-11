package com.dwolla.awssdk.utils

import com.dwolla.awssdk.utils.ScalaAsyncHandler.Implicits._

import scala.concurrent.{ExecutionContext, Future}
import scala.language.reflectiveCalls
import scala.reflect.ClassTag

object PaginatedResponseRetriever {

  class PaginatedResponseRetriever[Req <: PaginatedRequest, Res <: PaginatedResult](requestFactory: () ⇒ Req) {
    def via(awsAsyncFunction: AwsAsyncFunction[Req, Res])(implicit ec: ExecutionContext): Future[List[Res]] = {
      def impl(nextToken: Option[String] = Some(null), acc: List[Res] = Nil): Future[List[Res]] = nextToken match {
        case None ⇒ Future.successful(acc.reverse)
        case Some(token) ⇒
          val req = requestFactory()
          req.setNextToken(token)
          req.via(awsAsyncFunction)
            .flatMap(res ⇒ impl(Option(res.getNextToken()), res :: acc))
      }

      impl()
    }
  }

  def fetchAll[Req <: PaginatedRequest, Res <: PaginatedResult](requestFactory: () ⇒ Req,
                                                                awsAsyncFunction: AwsAsyncFunction[Req, Res])
                                                               (implicit ec: ExecutionContext): Future[List[Res]] = fetchAllWithRequestsLike(requestFactory).via(awsAsyncFunction)

  def fetchAllWithRequestsLike[Req <: PaginatedRequest, Res <: PaginatedResult](requestFactory: () ⇒ Req): PaginatedResponseRetriever[Req, Res] =
    new PaginatedResponseRetriever(requestFactory)

  def fetchAllWithDefaultRequestsVia[Req <: PaginatedRequest : ClassTag, Res <: PaginatedResult](awsAsyncFunction: AwsAsyncFunction[Req, Res])
                                                                                                (implicit ec: ExecutionContext): Future[List[Res]] =
    fetchAllWithRequestsLike(() ⇒ implicitly[ClassTag[Req]].runtimeClass.newInstance().asInstanceOf[Req]).via(awsAsyncFunction)

}
