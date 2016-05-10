package com.dwolla.awssdk.utils

import com.amazonaws.AmazonWebServiceRequest
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

class ScalaAsyncHandlerSpec(implicit val executionEnv: ExecutionEnv) extends Specification {

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

}

class FakeAmazonWebServiceRequest extends AmazonWebServiceRequest
case class FakeResponse(value: Any)
case object IntentionalTestException extends RuntimeException("exception intentionally thrown by test", null, true, false)
