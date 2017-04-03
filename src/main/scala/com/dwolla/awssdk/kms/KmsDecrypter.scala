package com.dwolla.awssdk.kms

import java.nio.ByteBuffer

import com.amazonaws.regions.Regions
import com.amazonaws.regions.Regions.US_WEST_2
import com.amazonaws.services.kms.model.DecryptRequest
import com.amazonaws.services.kms.{AWSKMSAsync, AWSKMSAsyncClientBuilder}
import com.dwolla.awssdk.kms.KmsDecrypter._
import com.dwolla.awssdk.utils.ScalaAsyncHandler.Implicits._

import scala.concurrent.{ExecutionContext, Future}

class KmsDecrypter(region: Regions = US_WEST_2) extends AutoCloseable {
  protected lazy val asyncClient: AWSKMSAsync = AWSKMSAsyncClientBuilder.standard().withRegion(US_WEST_2).build()

  def decrypt[A](transformer: Transform[A], cryptotext: A)(implicit ec: ExecutionContext): Future[Array[Byte]] = new DecryptRequest()
    .withCiphertextBlob(ByteBuffer.wrap(transformer(cryptotext)))
    .via(asyncClient.decryptAsync)
    .map(_.getPlaintext.array())

  def decrypt[A](transform: Transform[A], cryptotexts: (String, A)*)(implicit ec: ExecutionContext): Future[Map[String, Array[Byte]]] = {
    Future.sequence(cryptotexts.map {
      case (name, cryptotext) ⇒ decrypt(transform, cryptotext).map(name → _)
    }).map(Map(_: _*))
  }

  def decryptBase64(cryptotexts: (String, String)*)(implicit ec: ExecutionContext): Future[Map[String, Array[Byte]]] = decrypt(base64DecodingTransform, cryptotexts: _*)

  override def close(): Unit = asyncClient.shutdown()
}

object KmsDecrypter {
  type Transform[A] = A ⇒ Array[Byte]

  val noopTransform: Transform[Array[Byte]] = (x: Array[Byte]) ⇒ x
  val base64DecodingTransform: Transform[String] = javax.xml.bind.DatatypeConverter.parseBase64Binary
}
