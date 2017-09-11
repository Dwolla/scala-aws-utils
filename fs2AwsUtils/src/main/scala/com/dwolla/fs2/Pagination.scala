package com.dwolla.fs2

import cats.Applicative
import fs2.{Segment, Stream}

import scala.language.{higherKinds, implicitConversions}

object Pagination {
  sealed trait MaybeNextPageToken[S]
  case class FirstPage[S]() extends MaybeNextPageToken[S]
  case class NextPage[S](token: S) extends MaybeNextPageToken[S]
  case class NoMorePages[S]() extends MaybeNextPageToken[S]

  object MaybeNextPageToken {
    implicit def optionToMaybeNextPageToken[S](o: Option[S]): MaybeNextPageToken[S] = o match {
      case Some(x) ⇒ NextPage(x)
      case None ⇒ NoMorePages[S]()
    }
  }

  def streamContainingAllPages[F[_], S, O](f: Option[S] ⇒ F[Option[(Segment[O, Unit], MaybeNextPageToken[S])]])
                                          (implicit F: Applicative[F]): Stream[F, O] =
    Stream.unfoldSegmentEval[F, MaybeNextPageToken[S], O](FirstPage[S]()) {
      case FirstPage() ⇒ f(None)
      case NextPage(token) ⇒ f(Option(token))
      case NoMorePages() ⇒ F.pure(None)
    }
}
