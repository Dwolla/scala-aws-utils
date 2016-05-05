package com.dwolla.awssdk.utils

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.concurrent.Promise

class ScalaAsyncHandler[A <: AmazonWebServiceRequest, B] extends AsyncHandler[A, B] {
  private val promise = Promise[B]
  val future = promise.future

  override def onError(exception: Exception): Unit = promise.failure(exception)

  override def onSuccess(request: A, result: B): Unit = promise.success(result)
}
