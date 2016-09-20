package com.dwolla.awssdk.utils

import java.util.concurrent.{Future ⇒ JFuture}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.concurrent.{Future, Promise}

class ScalaAsyncHandler[A <: AmazonWebServiceRequest, B] extends AsyncHandler[A, B] {
  private val promise = Promise[B]
  val future = promise.future

  override def onError(exception: Exception): Unit = promise.failure(exception)
  override def onSuccess(request: A, result: B): Unit = promise.success(result)
}

object ScalaAsyncHandler {
  object Implicits {
    implicit class RequestHolder[A <: AmazonWebServiceRequest](req: A) {
      /*
        This would be perfect if B could be provided as a mapping from A → the appropriate Result type. Then the type argument B could be removed from the signature and replaced
        throughout with the mapped value.
       */

      class BoundRequestHolder[B] {
        def via(body: (A, AsyncHandler[A, B]) ⇒ JFuture[B]): Future[B] = {
          val handler = new ScalaAsyncHandler[A, B]
          body(req, handler)
          handler.future
        }
      }

      def to[B]: BoundRequestHolder[B] = new BoundRequestHolder[B]
    }
  }
}
