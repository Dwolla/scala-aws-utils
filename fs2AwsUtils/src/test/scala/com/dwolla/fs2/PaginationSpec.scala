package com.dwolla.fs2

import cats.effect.IO
import fs2._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class PaginationSpec(implicit ee: ExecutionEnv) extends Specification {

  "Pagination" should {
    "evaluate f(s) before checking for None" >> {
      val lastPage = 2

      def fetchPage(maybeNextToken: Option[Int]): IO[(Segment[Int, Unit], Option[Int])] = {
        def nextValue(i: Int): IO[(Segment[Int, Unit], Option[Int])] = IO((Segment(i), Option(i + 1)))

        maybeNextToken match {
          case Some(i) if i < lastPage ⇒ nextValue(i)
          case Some(`lastPage`) ⇒ IO((Segment(lastPage), None))
          case None ⇒ nextValue(0)
        }
      }

      val stream: Stream[IO, Int] = Pagination.streamContainingAllPages(fetchPage)

      stream.runLog.unsafeToFuture() must be_==(0 to lastPage).await
    }
  }

}
