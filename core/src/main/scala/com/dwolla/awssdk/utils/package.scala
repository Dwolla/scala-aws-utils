package com.dwolla.awssdk

import java.util.concurrent.{Future ⇒ JFuture}

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler

import scala.language.reflectiveCalls

package object utils {
  //noinspection ScalaUnusedSymbol
  type PaginatedRequest = AmazonWebServiceRequest {def setNextToken(s: String): Unit}
  //noinspection AccessorLikeMethodIsEmptyParen,ScalaUnusedSymbol
  type PaginatedResult = {def getNextToken(): String}
  type AwsAsyncFunction[Req <: AmazonWebServiceRequest, Res] = (Req, AsyncHandler[Req, Res]) ⇒ JFuture[Res]
}
