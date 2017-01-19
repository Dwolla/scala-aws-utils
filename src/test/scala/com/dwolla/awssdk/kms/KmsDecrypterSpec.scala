package com.dwolla.awssdk.kms

import java.nio.ByteBuffer
import java.util.concurrent.{Future ⇒ JFuture}

import com.amazonaws.handlers.AsyncHandler
import com.amazonaws.regions.Region
import com.amazonaws.services.kms.AWSKMSAsync
import com.amazonaws.services.kms.model._
import com.amazonaws.{AmazonWebServiceRequest, ResponseMetadata}
import com.dwolla.awssdk.kms.KmsDecrypter.Transform
import org.specs2.concurrent.ExecutionEnv
import org.specs2.mock.Mockito
import org.specs2.mutable.Specification
import org.specs2.specification.Scope

import scala.concurrent.{ExecutionContext, Future, Promise}

class KmsDecrypterSpec(implicit ee: ExecutionEnv) extends Specification with Mockito {

  trait MockSetup extends Scope {
    val mockClient = mock[AWSKMSAsync]

    val testInstance = new KmsDecrypter() {
      override protected lazy val asyncClient = mockClient
    }
  }

  trait FakeSetup extends Scope {
    val fakeClient = new FakeAWSKMSAsync

    val testInstance = new KmsDecrypter() {
      override protected lazy val asyncClient = fakeClient
    }
  }

  "KmsDecrypter" should {
    "closing" >> {
      "be autocloseable" in new MockSetup {
        testInstance must beAnInstanceOf[AutoCloseable]
      }

      "shut down the client when closing" in new MockSetup {
        testInstance.close()

        there was one(mockClient).shutdown()
      }
    }

    "decryption" >> {

      "decrypt by passing the right values to the AWS SDK" in new FakeSetup {
        val inputBytes = randomBytes()
        val promisedInputBytes = Promise[Array[Byte]]
        val expectedBytes = "it worked".getBytes("UTF-8")

        fakeClient.promisedAction.success((req: DecryptRequest, handler: AsyncHandler[DecryptRequest, DecryptResult]) ⇒ {
          promisedInputBytes.success(req.getCiphertextBlob.array())
          handler.onSuccess(req, new DecryptResult().withPlaintext(ByteBuffer.wrap(expectedBytes)))
        })

        val output = testInstance.decrypt(KmsDecrypter.noopTransform, inputBytes)

        output must be_==(expectedBytes).await
        promisedInputBytes.future must be_==(inputBytes).await
      }

      "bulk decrypt by passing the right values to the decrypt method, and sequencing the responses" >> {
        val decryptMap = Map("crypto1" → "plaintext1", "crypto2" → "plaintext2").transform((_, value) ⇒ value.getBytes("UTF-8"))

        val testInstance = new KmsDecrypter() {
          override def decrypt[A](transformer: Transform[A], cryptotext: A)
                                 (implicit ec: ExecutionContext): Future[Array[Byte]] = {
            if (transformer == KmsDecrypter.base64DecodingTransform) Future.successful(decryptMap(cryptotext.asInstanceOf[String]))
            else Future.failed(new IllegalArgumentException("wrong transformer was passed"))
          }
        }

        val output = testInstance.decrypt(KmsDecrypter.base64DecodingTransform, "first" → "crypto1", "second" → "crypto2")

        output must havePair("first" → decryptMap("crypto1")).await
        output must havePair("second" → decryptMap("crypto2")).await
      }

      "Base64 bulk decrypt by passing the right transform to bulk decrypt" >> {
        val passedTransform = Promise[Transform[_]]
        val passedCryptoText = Promise[Seq[(String, _)]]

        val testInstance = new KmsDecrypter() {
          override def decrypt[A](transform: Transform[A], cryptotexts: (String, A)*)
                                 (implicit ec: ExecutionContext): Future[Map[String, Array[Byte]]] = {
            passedTransform.success(transform)
            passedCryptoText.success(cryptotexts)

            Future.successful(Map.empty[String, Array[Byte]])
          }
        }

        testInstance.decryptBase64("first" → "crypto1", "second" → "crypto2")

        passedTransform.future must be_==(KmsDecrypter.base64DecodingTransform).await
        passedCryptoText.future must be_==(Seq("first" → "crypto1", "second" → "crypto2")).await
      }

    }

    "transforms" >> {

      "no-op transform" should {
        "do nothing" >> {
          val input = randomBytes()
          val output = KmsDecrypter.noopTransform(input)

          output must_== input
        }
      }

      "base64 decoding transform" should {
        "decode the Base64-encoded string" in new MockSetup {
          val expectedBytes = randomBytes()
          val input = javax.xml.bind.DatatypeConverter.printBase64Binary(expectedBytes)

          val output = KmsDecrypter.base64DecodingTransform(input)

          output must_== expectedBytes
        }
      }
    }
  }

  def randomBytes(size: Int = 20) = {
    val bytes = new Array[Byte](size)
    scala.util.Random.nextBytes(bytes)
    bytes
  }

}

//noinspection NotImplementedCode
class FakeAWSKMSAsync extends AWSKMSAsync {
  val promisedDecryptRequest = Promise[DecryptRequest]
  val promisedAction = Promise[(DecryptRequest, AsyncHandler[DecryptRequest, DecryptResult]) ⇒ Unit]

  override def decryptAsync(decryptRequest: DecryptRequest, asyncHandler: AsyncHandler[DecryptRequest, DecryptResult]): JFuture[DecryptResult] = {
    promisedDecryptRequest.success(decryptRequest)

    if (promisedAction.isCompleted) promisedAction.future.value.get.get(decryptRequest, asyncHandler)
    else throw new RuntimeException("must set promisedAction before calling decryptAsync")

    null
  }

  override def enableKeyRotationAsync(enableKeyRotationRequest: EnableKeyRotationRequest): JFuture[EnableKeyRotationResult] = ???

  override def enableKeyRotationAsync(enableKeyRotationRequest: EnableKeyRotationRequest,
                                      asyncHandler: AsyncHandler[EnableKeyRotationRequest, EnableKeyRotationResult]): JFuture[EnableKeyRotationResult] = ???

  override def getKeyRotationStatusAsync(getKeyRotationStatusRequest: GetKeyRotationStatusRequest): JFuture[GetKeyRotationStatusResult] = ???

  override def getKeyRotationStatusAsync(getKeyRotationStatusRequest: GetKeyRotationStatusRequest,
                                         asyncHandler: AsyncHandler[GetKeyRotationStatusRequest, GetKeyRotationStatusResult]): JFuture[GetKeyRotationStatusResult] = ???

  override def importKeyMaterialAsync(importKeyMaterialRequest: ImportKeyMaterialRequest): JFuture[ImportKeyMaterialResult] = ???

  override def importKeyMaterialAsync(importKeyMaterialRequest: ImportKeyMaterialRequest,
                                      asyncHandler: AsyncHandler[ImportKeyMaterialRequest, ImportKeyMaterialResult]): JFuture[ImportKeyMaterialResult] = ???

  override def scheduleKeyDeletionAsync(scheduleKeyDeletionRequest: ScheduleKeyDeletionRequest): JFuture[ScheduleKeyDeletionResult] = ???

  override def scheduleKeyDeletionAsync(scheduleKeyDeletionRequest: ScheduleKeyDeletionRequest,
                                        asyncHandler: AsyncHandler[ScheduleKeyDeletionRequest, ScheduleKeyDeletionResult]): JFuture[ScheduleKeyDeletionResult] = ???

  override def listKeyPoliciesAsync(listKeyPoliciesRequest: ListKeyPoliciesRequest): JFuture[ListKeyPoliciesResult] = ???

  override def listKeyPoliciesAsync(listKeyPoliciesRequest: ListKeyPoliciesRequest,
                                    asyncHandler: AsyncHandler[ListKeyPoliciesRequest, ListKeyPoliciesResult]): JFuture[ListKeyPoliciesResult] = ???

  override def createAliasAsync(createAliasRequest: CreateAliasRequest): JFuture[CreateAliasResult] = ???

  override def createAliasAsync(createAliasRequest: CreateAliasRequest, asyncHandler: AsyncHandler[CreateAliasRequest, CreateAliasResult]): JFuture[CreateAliasResult] = ???

  override def decryptAsync(decryptRequest: DecryptRequest): JFuture[DecryptResult] = ???

  override def cancelKeyDeletionAsync(cancelKeyDeletionRequest: CancelKeyDeletionRequest): JFuture[CancelKeyDeletionResult] = ???

  override def cancelKeyDeletionAsync(cancelKeyDeletionRequest: CancelKeyDeletionRequest,
                                      asyncHandler: AsyncHandler[CancelKeyDeletionRequest, CancelKeyDeletionResult]): JFuture[CancelKeyDeletionResult] = ???

  override def deleteAliasAsync(deleteAliasRequest: DeleteAliasRequest): JFuture[DeleteAliasResult] = ???

  override def deleteAliasAsync(deleteAliasRequest: DeleteAliasRequest, asyncHandler: AsyncHandler[DeleteAliasRequest, DeleteAliasResult]): JFuture[DeleteAliasResult] = ???

  override def createGrantAsync(createGrantRequest: CreateGrantRequest): JFuture[CreateGrantResult] = ???

  override def createGrantAsync(createGrantRequest: CreateGrantRequest, asyncHandler: AsyncHandler[CreateGrantRequest, CreateGrantResult]): JFuture[CreateGrantResult] = ???

  override def revokeGrantAsync(revokeGrantRequest: RevokeGrantRequest): JFuture[RevokeGrantResult] = ???

  override def revokeGrantAsync(revokeGrantRequest: RevokeGrantRequest, asyncHandler: AsyncHandler[RevokeGrantRequest, RevokeGrantResult]): JFuture[RevokeGrantResult] = ???

  override def listKeysAsync(listKeysRequest: ListKeysRequest): JFuture[ListKeysResult] = ???

  override def listKeysAsync(listKeysRequest: ListKeysRequest, asyncHandler: AsyncHandler[ListKeysRequest, ListKeysResult]): JFuture[ListKeysResult] = ???

  override def listKeysAsync(): JFuture[ListKeysResult] = ???

  override def listKeysAsync(asyncHandler: AsyncHandler[ListKeysRequest, ListKeysResult]): JFuture[ListKeysResult] = ???

  override def listRetirableGrantsAsync(listRetirableGrantsRequest: ListRetirableGrantsRequest): JFuture[ListRetirableGrantsResult] = ???

  override def listRetirableGrantsAsync(listRetirableGrantsRequest: ListRetirableGrantsRequest,
                                        asyncHandler: AsyncHandler[ListRetirableGrantsRequest, ListRetirableGrantsResult]): JFuture[ListRetirableGrantsResult] = ???

  override def listAliasesAsync(listAliasesRequest: ListAliasesRequest): JFuture[ListAliasesResult] = ???

  override def listAliasesAsync(listAliasesRequest: ListAliasesRequest, asyncHandler: AsyncHandler[ListAliasesRequest, ListAliasesResult]): JFuture[ListAliasesResult] = ???

  override def listAliasesAsync(): JFuture[ListAliasesResult] = ???

  override def listAliasesAsync(asyncHandler: AsyncHandler[ListAliasesRequest, ListAliasesResult]): JFuture[ListAliasesResult] = ???

  override def generateDataKeyWithoutPlaintextAsync(generateDataKeyWithoutPlaintextRequest: GenerateDataKeyWithoutPlaintextRequest): JFuture[GenerateDataKeyWithoutPlaintextResult] = ???

  override def generateDataKeyWithoutPlaintextAsync(generateDataKeyWithoutPlaintextRequest: GenerateDataKeyWithoutPlaintextRequest,
                                                    asyncHandler: AsyncHandler[GenerateDataKeyWithoutPlaintextRequest, GenerateDataKeyWithoutPlaintextResult]): JFuture[GenerateDataKeyWithoutPlaintextResult] = ???

  override def generateDataKeyAsync(generateDataKeyRequest: GenerateDataKeyRequest): JFuture[GenerateDataKeyResult] = ???

  override def generateDataKeyAsync(generateDataKeyRequest: GenerateDataKeyRequest,
                                    asyncHandler: AsyncHandler[GenerateDataKeyRequest, GenerateDataKeyResult]): JFuture[GenerateDataKeyResult] = ???

  override def putKeyPolicyAsync(putKeyPolicyRequest: PutKeyPolicyRequest): JFuture[PutKeyPolicyResult] = ???

  override def putKeyPolicyAsync(putKeyPolicyRequest: PutKeyPolicyRequest, asyncHandler: AsyncHandler[PutKeyPolicyRequest, PutKeyPolicyResult]): JFuture[PutKeyPolicyResult] = ???

  override def createKeyAsync(createKeyRequest: CreateKeyRequest): JFuture[CreateKeyResult] = ???

  override def createKeyAsync(createKeyRequest: CreateKeyRequest, asyncHandler: AsyncHandler[CreateKeyRequest, CreateKeyResult]): JFuture[CreateKeyResult] = ???

  override def createKeyAsync(): JFuture[CreateKeyResult] = ???

  override def createKeyAsync(asyncHandler: AsyncHandler[CreateKeyRequest, CreateKeyResult]): JFuture[CreateKeyResult] = ???

  override def encryptAsync(encryptRequest: EncryptRequest): JFuture[EncryptResult] = ???

  override def encryptAsync(encryptRequest: EncryptRequest, asyncHandler: AsyncHandler[EncryptRequest, EncryptResult]): JFuture[EncryptResult] = ???

  override def updateKeyDescriptionAsync(updateKeyDescriptionRequest: UpdateKeyDescriptionRequest): JFuture[UpdateKeyDescriptionResult] = ???

  override def updateKeyDescriptionAsync(updateKeyDescriptionRequest: UpdateKeyDescriptionRequest,
                                         asyncHandler: AsyncHandler[UpdateKeyDescriptionRequest, UpdateKeyDescriptionResult]): JFuture[UpdateKeyDescriptionResult] = ???

  override def disableKeyRotationAsync(disableKeyRotationRequest: DisableKeyRotationRequest): JFuture[DisableKeyRotationResult] = ???

  override def disableKeyRotationAsync(disableKeyRotationRequest: DisableKeyRotationRequest,
                                       asyncHandler: AsyncHandler[DisableKeyRotationRequest, DisableKeyRotationResult]): JFuture[DisableKeyRotationResult] = ???

  override def getParametersForImportAsync(getParametersForImportRequest: GetParametersForImportRequest): JFuture[GetParametersForImportResult] = ???

  override def getParametersForImportAsync(getParametersForImportRequest: GetParametersForImportRequest,
                                           asyncHandler: AsyncHandler[GetParametersForImportRequest, GetParametersForImportResult]): JFuture[GetParametersForImportResult] = ???

  override def listGrantsAsync(listGrantsRequest: ListGrantsRequest): JFuture[ListGrantsResult] = ???

  override def listGrantsAsync(listGrantsRequest: ListGrantsRequest, asyncHandler: AsyncHandler[ListGrantsRequest, ListGrantsResult]): JFuture[ListGrantsResult] = ???

  override def generateRandomAsync(generateRandomRequest: GenerateRandomRequest): JFuture[GenerateRandomResult] = ???

  override def generateRandomAsync(generateRandomRequest: GenerateRandomRequest,
                                   asyncHandler: AsyncHandler[GenerateRandomRequest, GenerateRandomResult]): JFuture[GenerateRandomResult] = ???

  override def generateRandomAsync(): JFuture[GenerateRandomResult] = ???

  override def generateRandomAsync(asyncHandler: AsyncHandler[GenerateRandomRequest, GenerateRandomResult]): JFuture[GenerateRandomResult] = ???

  override def enableKeyAsync(enableKeyRequest: EnableKeyRequest): JFuture[EnableKeyResult] = ???

  override def enableKeyAsync(enableKeyRequest: EnableKeyRequest, asyncHandler: AsyncHandler[EnableKeyRequest, EnableKeyResult]): JFuture[EnableKeyResult] = ???

  override def updateAliasAsync(updateAliasRequest: UpdateAliasRequest): JFuture[UpdateAliasResult] = ???

  override def updateAliasAsync(updateAliasRequest: UpdateAliasRequest, asyncHandler: AsyncHandler[UpdateAliasRequest, UpdateAliasResult]): JFuture[UpdateAliasResult] = ???

  override def describeKeyAsync(describeKeyRequest: DescribeKeyRequest): JFuture[DescribeKeyResult] = ???

  override def describeKeyAsync(describeKeyRequest: DescribeKeyRequest, asyncHandler: AsyncHandler[DescribeKeyRequest, DescribeKeyResult]): JFuture[DescribeKeyResult] = ???

  override def retireGrantAsync(retireGrantRequest: RetireGrantRequest): JFuture[RetireGrantResult] = ???

  override def retireGrantAsync(retireGrantRequest: RetireGrantRequest, asyncHandler: AsyncHandler[RetireGrantRequest, RetireGrantResult]): JFuture[RetireGrantResult] = ???

  override def retireGrantAsync(): JFuture[RetireGrantResult] = ???

  override def retireGrantAsync(asyncHandler: AsyncHandler[RetireGrantRequest, RetireGrantResult]): JFuture[RetireGrantResult] = ???

  override def reEncryptAsync(reEncryptRequest: ReEncryptRequest): JFuture[ReEncryptResult] = ???

  override def reEncryptAsync(reEncryptRequest: ReEncryptRequest, asyncHandler: AsyncHandler[ReEncryptRequest, ReEncryptResult]): JFuture[ReEncryptResult] = ???

  override def getKeyPolicyAsync(getKeyPolicyRequest: GetKeyPolicyRequest): JFuture[GetKeyPolicyResult] = ???

  override def getKeyPolicyAsync(getKeyPolicyRequest: GetKeyPolicyRequest, asyncHandler: AsyncHandler[GetKeyPolicyRequest, GetKeyPolicyResult]): JFuture[GetKeyPolicyResult] = ???

  override def deleteImportedKeyMaterialAsync(deleteImportedKeyMaterialRequest: DeleteImportedKeyMaterialRequest): JFuture[DeleteImportedKeyMaterialResult] = ???

  override def deleteImportedKeyMaterialAsync(deleteImportedKeyMaterialRequest: DeleteImportedKeyMaterialRequest,
                                              asyncHandler: AsyncHandler[DeleteImportedKeyMaterialRequest, DeleteImportedKeyMaterialResult]): JFuture[DeleteImportedKeyMaterialResult] = ???

  override def disableKeyAsync(disableKeyRequest: DisableKeyRequest): JFuture[DisableKeyResult] = ???

  override def disableKeyAsync(disableKeyRequest: DisableKeyRequest, asyncHandler: AsyncHandler[DisableKeyRequest, DisableKeyResult]): JFuture[DisableKeyResult] = ???

  override def putKeyPolicy(putKeyPolicyRequest: PutKeyPolicyRequest): PutKeyPolicyResult = ???

  override def enableKey(enableKeyRequest: EnableKeyRequest): EnableKeyResult = ???

  override def setEndpoint(endpoint: String): Unit = ???

  override def generateRandom(generateRandomRequest: GenerateRandomRequest): GenerateRandomResult = ???

  override def generateRandom(): GenerateRandomResult = ???

  override def getParametersForImport(getParametersForImportRequest: GetParametersForImportRequest): GetParametersForImportResult = ???

  override def shutdown(): Unit = ???

  override def enableKeyRotation(enableKeyRotationRequest: EnableKeyRotationRequest): EnableKeyRotationResult = ???

  override def listKeyPolicies(listKeyPoliciesRequest: ListKeyPoliciesRequest): ListKeyPoliciesResult = ???

  override def decrypt(decryptRequest: DecryptRequest): DecryptResult = ???

  override def deleteAlias(deleteAliasRequest: DeleteAliasRequest): DeleteAliasResult = ???

  override def listGrants(listGrantsRequest: ListGrantsRequest): ListGrantsResult = ???

  override def disableKeyRotation(disableKeyRotationRequest: DisableKeyRotationRequest): DisableKeyRotationResult = ???

  override def importKeyMaterial(importKeyMaterialRequest: ImportKeyMaterialRequest): ImportKeyMaterialResult = ???

  override def cancelKeyDeletion(cancelKeyDeletionRequest: CancelKeyDeletionRequest): CancelKeyDeletionResult = ???

  override def generateDataKey(generateDataKeyRequest: GenerateDataKeyRequest): GenerateDataKeyResult = ???

  override def setRegion(region: Region): Unit = ???

  override def encrypt(encryptRequest: EncryptRequest): EncryptResult = ???

  override def createAlias(createAliasRequest: CreateAliasRequest): CreateAliasResult = ???

  override def deleteImportedKeyMaterial(deleteImportedKeyMaterialRequest: DeleteImportedKeyMaterialRequest): DeleteImportedKeyMaterialResult = ???

  override def createKey(createKeyRequest: CreateKeyRequest): CreateKeyResult = ???

  override def createKey(): CreateKeyResult = ???

  override def updateAlias(updateAliasRequest: UpdateAliasRequest): UpdateAliasResult = ???

  override def scheduleKeyDeletion(scheduleKeyDeletionRequest: ScheduleKeyDeletionRequest): ScheduleKeyDeletionResult = ???

  override def describeKey(describeKeyRequest: DescribeKeyRequest): DescribeKeyResult = ???

  override def createGrant(createGrantRequest: CreateGrantRequest): CreateGrantResult = ???

  override def revokeGrant(revokeGrantRequest: RevokeGrantRequest): RevokeGrantResult = ???

  override def getKeyRotationStatus(getKeyRotationStatusRequest: GetKeyRotationStatusRequest): GetKeyRotationStatusResult = ???

  override def getKeyPolicy(getKeyPolicyRequest: GetKeyPolicyRequest): GetKeyPolicyResult = ???

  override def disableKey(disableKeyRequest: DisableKeyRequest): DisableKeyResult = ???

  override def retireGrant(retireGrantRequest: RetireGrantRequest): RetireGrantResult = ???

  override def retireGrant(): RetireGrantResult = ???

  override def listRetirableGrants(listRetirableGrantsRequest: ListRetirableGrantsRequest): ListRetirableGrantsResult = ???

  override def getCachedResponseMetadata(request: AmazonWebServiceRequest): ResponseMetadata = ???

  override def listKeys(listKeysRequest: ListKeysRequest): ListKeysResult = ???

  override def listKeys(): ListKeysResult = ???

  override def listAliases(listAliasesRequest: ListAliasesRequest): ListAliasesResult = ???

  override def listAliases(): ListAliasesResult = ???

  override def generateDataKeyWithoutPlaintext(generateDataKeyWithoutPlaintextRequest: GenerateDataKeyWithoutPlaintextRequest): GenerateDataKeyWithoutPlaintextResult = ???

  override def updateKeyDescription(updateKeyDescriptionRequest: UpdateKeyDescriptionRequest): UpdateKeyDescriptionResult = ???

  override def reEncrypt(reEncryptRequest: ReEncryptRequest): ReEncryptResult = ???
}