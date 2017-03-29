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
  type AwsAsyncFunction[Request <: AmazonWebServiceRequest, Result] = (Request, AsyncHandler[Request, Result]) ⇒ JFuture[Result]

  object Implicits {
    implicit class RequestHolder[S <: AmazonWebServiceRequest](req: S) {
      class BoundRequestHolder[T] {
        def via(body: (S, AsyncHandler[S, T]) ⇒ JFuture[T]): Future[T] = {
          val handler = new ScalaAsyncHandler[S, T]
          body(req, handler)
          handler.future
        }
      }

      @deprecated(message = "use RequestHolder.via(…) directly", since = "1.3.0")
      def to[T]: BoundRequestHolder[T] = new BoundRequestHolder[T]

      def via[T](body: (S, AsyncHandler[S, T]) ⇒ JFuture[T]): Future[T] = new BoundRequestHolder[T].via(body)
    }
  }
}
