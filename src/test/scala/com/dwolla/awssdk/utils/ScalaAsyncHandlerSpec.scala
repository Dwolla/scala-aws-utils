package com.dwolla.awssdk.utils

import com.amazonaws.AmazonWebServiceRequest
import com.amazonaws.handlers.AsyncHandler
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ScalaAsyncHandlerSpec(implicit val executionEnv: ExecutionEnv) extends Specification with Mockito {

  trait Setup extends Scope {
    val testClass = new ScalaAsyncHandler[FakeAmazonWebServiceRequest, FakeResponse]
  }

  "ScalaAsyncHandler" should {

    "complete successfully by fulfilling promise" in new Setup {
      testClass.onSuccess(null, FakeResponse("success!"))

      testClass.future must be_==(FakeResponse("success!")).await
    }

    "complete in error by fulfilling promise" in new Setup {
      testClass.onError(IntentionalTestException)

      testClass.future must throwA(IntentionalTestException).await
    }
  }

  "Implicits.RequestHolder" should {
    import ScalaAsyncHandler.Implicits._

    "convert AmazonWebServicesRequest to a Future[Response]" in {
      val fakeClient = new FakeAmazonAsyncClient(Right(FakeResponse("success!")))

      val output = new FakeAmazonWebServiceRequest().via(fakeClient.fakeAsync)

      output must be_==(FakeResponse("success!")).await
    }

    "capture exceptions from passed method" in {
      val fakeClient = new FakeAmazonAsyncClient(Left(IntentionalTestException))

      val output = new FakeAmazonWebServiceRequest().via(fakeClient.fakeAsync)

      output must throwA(IntentionalTestException).await
    }

    "convert AmazonWebServicesRequest to a Future[Response]" in {
      val fakeClient = new FakeAmazonAsyncClient(Right(FakeResponse("success!")))

      val output = new FakeAmazonWebServiceRequest().via(fakeClient.fakeAsync)

      output must be_==(FakeResponse("success!")).await
    }

    "capture exceptions from passed method" in {
      val fakeClient = new FakeAmazonAsyncClient(Left(IntentionalTestException))

      val output = new FakeAmazonWebServiceRequest().via(fakeClient.fakeAsync)

      output must throwA(IntentionalTestException).await
    }
  }
}

class FakeAmazonWebServiceRequest extends AmazonWebServiceRequest
case class FakeResponse(value: Any)
case object IntentionalTestException extends RuntimeException("exception intentionally thrown by test", null, true, false)

class FakeAmazonAsyncClient(expectedResponse: Either[Exception, FakeResponse]) {
  def fakeAsync(req: FakeAmazonWebServiceRequest, handler: AsyncHandler[FakeAmazonWebServiceRequest, FakeResponse]): java.util.concurrent.Future[FakeResponse] = {
    expectedResponse match {
      case Left(ex) ⇒ handler.onError(ex)
      case Right(res) ⇒ handler.onSuccess(req, res)
    }

    null
  }
}
