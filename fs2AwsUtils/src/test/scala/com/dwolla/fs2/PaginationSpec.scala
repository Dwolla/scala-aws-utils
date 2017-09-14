package com.dwolla.fs2

import cats.effect.IO
import fs2._
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mutable.Specification

class PaginationSpec(implicit ee: ExecutionEnv) extends Specification {

  "Pagination" should {
    "unfold the stream when the page function returns segments" >> {
      val lastPage = 2

      def fetchPage(maybeNextToken: Option[Int]): IO[(Segment[Int, Unit], Option[Int])] = {
        def segment(i: Int) = Segment((0 until 10).map(_ + (i * 10)): _*)
        def nextValue(i: Int): IO[(Segment[Int, Unit], Option[Int])] = IO(segment(i), Option(i + 1))

        maybeNextToken match {
          case Some(i) if i < lastPage ⇒ nextValue(i)
          case Some(`lastPage`) ⇒ IO((segment(lastPage), None))
          case None ⇒ nextValue(0)
        }
      }

      val stream: Stream[IO, Int] = Pagination.offsetUnfoldSegmentEval(fetchPage)

      stream.runLog.unsafeRunSync() must be_==(0 until (lastPage + 1) * 10)
    }

    "unfold the stream when the page function returns elements" >> {
      val lastPage = 2

      def fetchPage(maybeNextToken: Option[Int]): IO[(Int, Option[Int])] = {
        def nextValue(i: Int): IO[(Int, Option[Int])] = IO((i, Option(i + 1)))

        maybeNextToken match {
          case Some(i) if i < lastPage ⇒ nextValue(i)
          case Some(`lastPage`) ⇒ IO((lastPage, None))
          case None ⇒ nextValue(0)
        }
      }

      val stream: Stream[IO, Int] = Pagination.offsetUnfoldEval(fetchPage)

      stream.runLog.unsafeRunSync() must be_==(0 to lastPage)
    }
  }

}
