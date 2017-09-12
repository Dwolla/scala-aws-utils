package com.dwolla.fs2

import cats.Applicative
import cats.implicits._
import fs2.{Segment, Stream}

import scala.language.{higherKinds, implicitConversions}

object Pagination {
  private sealed trait PageIndicator[S]
  private case class FirstPage[S]() extends PageIndicator[S]
  private case class NextPage[S](token: S) extends PageIndicator[S]
  private case class NoMorePages[S]() extends PageIndicator[S]

  def offsetUnfoldSegmentEval[F[_], S, O](f: Option[S] ⇒ F[(Segment[O, Unit], Option[S])])
                                         (implicit F: Applicative[F]): Stream[F, O] = {
    def fetchPage(maybeNextPageToken: Option[S]): F[Option[(Segment[O, Unit], PageIndicator[S])]] = {
      f(maybeNextPageToken).map {
        case (segment, Some(nextToken)) ⇒ Option((segment, NextPage(nextToken)))
        case (segment, None) ⇒ Option((segment, NoMorePages[S]()))
      }
    }

    Stream.unfoldSegmentEval[F, PageIndicator[S], O](FirstPage[S]()) {
      case FirstPage() ⇒ fetchPage(None)
      case NextPage(token) ⇒ fetchPage(Some(token))
      case NoMorePages() ⇒ F.pure(None)
    }
  }

  def offsetUnfoldEval[F[_] : Applicative, S, O](f: Option[S] ⇒ F[(O, Option[S])]): Stream[F, O] =
    offsetUnfoldSegmentEval[F, S, O](f(_).map {
      case (o, maybeNextToken) ⇒ (Segment(o), maybeNextToken)
    })
}
