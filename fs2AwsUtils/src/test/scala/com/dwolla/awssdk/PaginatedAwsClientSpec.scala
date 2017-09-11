package com.dwolla.awssdk

import cats.effect.IO
import com.dwolla.fs2.Pagination
import com.dwolla.fs2.Pagination.{MaybeNextPageToken, NoMorePages}
import com.dwolla.fs2.Pagination.MaybeNextPageToken._
import org.specs2.mutable.Specification
import fs2._
import org.specs2.concurrent.ExecutionEnv

class PaginatedAwsClientSpec(implicit ee: ExecutionEnv) extends Specification {

  "Unfold" should {
    "unfold the stream when the page function returns segments" >> {
      val lastPage = 2

      def fetchPage(maybeNextToken: Option[Int]): IO[Option[(Segment[Int, Unit], MaybeNextPageToken[Int])]] = {
        def segment(i: Int) = Segment((0 until 10).map(_ + (i * 10)): _*)
        def nextValue(i: Int): IO[Option[(Segment[Int, Unit], MaybeNextPageToken[Int])]] = IO(Option((segment(i), Option(i + 1))))

        maybeNextToken match {
          case Some(i) if i < lastPage ⇒ nextValue(i)
          case Some(`lastPage`) ⇒ IO(Option((segment(lastPage), NoMorePages[Int]())))
          case None ⇒ nextValue(0)
        }
      }

      val stream: Stream[IO, Int] = Pagination.streamContainingAllPages(fetchPage)

      stream.runLog.unsafeToFuture() must be_==(0 until (lastPage + 1) * 10).await
    }
  }

}
