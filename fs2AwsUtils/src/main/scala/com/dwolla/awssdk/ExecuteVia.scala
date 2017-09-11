package com.dwolla.awssdk

import cats.effect.Effect
import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import com.dwolla.awssdk.utils.AwsAsyncFunction

import scala.language.higherKinds

object ExecuteVia {
  implicit class RequestHolder[Req <: AmazonWebServiceRequest](req: Req) {
    class EffectTypeBinding[F[_]] {
      def apply[Res](awsAsyncFunction: AwsAsyncFunction[Req, Res])(implicit F: Effect[F]): F[Res] = F.async[Res] { callback ⇒
        awsAsyncFunction(req, new AsyncHandler[Req, Res] {
          override def onError(exception: Exception): Unit = callback(Left(exception))
          override def onSuccess(request: Req, result: Res): Unit = callback(Right(result))
        })

        ()
      }
    }

    def executeVia[F[_]] = new EffectTypeBinding[F]
  }
}
