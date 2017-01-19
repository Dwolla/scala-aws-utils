package com.dwolla.awssdk.utils

import java.util.concurrent.{Future ⇒ JFuture}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.concurrent.{Future, Promise}

class ScalaAsyncHandler[A <: AmazonWebServiceRequest, B] extends AsyncHandler[A, B] {
  private val promise = Promise[B]
  val future: Future[B] = promise.future

  override def onError(exception: Exception): Unit = promise.failure(exception)
  override def onSuccess(request: A, result: B): Unit = promise.success(result)
}

object ScalaAsyncHandler {
  object Implicits {
    implicit class RequestHolder[S <: AmazonWebServiceRequest](req: S) {
      /*
        This would be perfect if B could be provided as a mapping from S → the appropriate Result type. Then the type argument T could be removed from the signature and replaced
        throughout with the mapped value.
       */
      class BoundRequestHolder[T] {
        def via(body: (S, AsyncHandler[S, T]) ⇒ JFuture[T]): Future[T] = {
          val handler = new ScalaAsyncHandler[S, T]
          body(req, handler)
          handler.future
        }
      }

      def to[T]: BoundRequestHolder[T] = new BoundRequestHolder[T]
    }
  }
}
