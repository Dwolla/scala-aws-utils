# Scala AWS Utilities

[![Travis](https://img.shields.io/travis/Dwolla/scala-aws-utils.svg?style=flat-square)](https://travis-ci.org/Dwolla/scala-aws-utils)
[![Bintray](https://img.shields.io/bintray/v/dwolla/maven/scala-aws-utils.svg?style=flat-square)](https://bintray.com/dwolla/maven/scala-aws-utils/view)
[![license](https://img.shields.io/github/license/Dwolla/scala-aws-utils.svg?style=flat-square)]()

Utilities package for working with the Java AWS SDKs from Scala and SBT.

Projects including this library will also need to explicitly include the AWS SDK libraries they will rely on, to avoid inadvertently importing more libraries than are required.

## Artifacts

#### Library

```scala
"com.dwolla" %% "scala-aws-utils" % "1.4.0"
```

#### Test Kit

```scala
"com.dwolla" %% "scala-aws-utils-testkit" % "1.4.0" % Test
```

The test kit provides helpers to make mocking the Amazon Async Clients easier. The clients have interfaces like

```java
java.util.concurrent.Future<DescribeInstancesResult> describeInstancesAsync(DescribeInstancesRequest describeInstancesRequest,
                                                                            AsyncHandler<DescribeInstancesRequest, DescribeInstancesResult> asyncHandler)
```

Because the method returns a Java `Future`, the “return value” for the function is actually returned by invoking a method on the passed `AsyncHandler`. This is inconvenient to set up.

Instead, the test kit provides a DSL for setting up the mocks. For example:

```scala
import com.dwolla.awssdk.AmazonAsyncMockingImplicits._

val mockClient = mock[AmazonEC2Async]

mockedMethod(mockClient.describeInstancesAsync) answers (
  new DescribeInstancesRequest().withInstanceIds("i-instance") → new DescribeInstancesResult(),
  new DescribeInstancesRequest() → new AmazonServiceException("access denied exception intentionally thrown by test")
)
```

The mocked method is provided with a mapping of `AmazonWebServiceRequest` instances of the correct type to the expected results. The results can either be actual result instances or exceptions to be thrown.
