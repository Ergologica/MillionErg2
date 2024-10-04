import org.ergoplatform.appkit._
import org.ergoplatform.appkit.impl.ErgoId
import scorex.crypto.hash.Blake2b256
import java.math.BigInteger

object RaffleContractExample {

  // Constants for customization
  val raffleTokenId = "your-raffle-token-id"
  val serviceTokenId = "your-service-token-id"
  val oracleId = "your-oracle-id"
  
  // Addresses
  val charityAddress = "your-charity-address"
  val serviceAddress = "your-service-address"
  val raffleProjectAddress = "your-raffle-project-address"
  val participantAddress = "your-participant-address"
  
  // Secrets (use proper secret management in production)
  val raffleProjectSecret = new BigInteger("your-raffle-project-secret")
  val participantSecret = new BigInteger("your-participant-secret")

  def makeRaffle(ctx: BlockchainContext): Unit = {
    // Example for a Successful Raffle
    {
      // Compile Winner Contract
      val winnerContract = ctx.compileContract(
        ConstantsBuilder.create()
          .build(),
        "winnerScriptHere" // Replace with actual winner script
      )

      val winnerErgoTree = winnerContract.getErgoTree
      val winnerScriptHash: Digest32 = Blake2b256(winnerErgoTree.bytes)

      // Compile Ticket Contract
      val ticketContract = ctx.compileContract(
        ConstantsBuilder.create()
          .item("deadlineHeight", 50) // Customize deadline
          .item("winnerScriptHash", winnerScriptHash)
          .item("ticketPrice", 1000000L) // Customize ticket price
          .item("projectPubKey", raffleProjectAddress.getPublicKey)
          .build(),
        "ticketScriptHere" // Replace with actual ticket script
      )

      // Compile Raffle Contract
      val raffleContract = ctx.compileContract(
        ConstantsBuilder.create()
          .item("tokenId", ErgoId.create(raffleTokenId).getBytes)
          .item("ticketPrice", 1000000L)
          .item("minToRaise", 600000000L) // Customize min to raise
          .item("deadlineHeight", 50) // Customize deadline
          .item("charityPubKey", charityAddress.getPublicKey)
          .item("servicePubKey", serviceAddress.getPublicKey)
          .item("winnerScriptHash", winnerScriptHash)
          .item("minFee", 1000000L) // Customize min fee
          .item("ticketScriptHash", Blake2b256(ticketContract.getErgoTree.bytes))
          .item("oracleNebulaNFT", ErgoId.create(oracleId).getBytes)
          .build(),
        "raffleContractScript" // Replace with actual raffle contract script
      )

      val txB = ctx.newTxBuilder()

      // Building Inputs and Outputs
      val in1 = txB.outBoxBuilder()
        .value(2000000L) // Customize input value
        .contract(raffleContract)
        .build()
        .convertToInputWith("your-input-box-id", 0) // Customize input box ID

      val out1 = txB.outBoxBuilder()
        .value(1000000L) // Customize output value
        .contract(new ErgoTreeContract(participantAddress._address.script))
        .build()

      // Create and sign the transaction
      val tx = txB.boxesToSpend(Seq(in1).asJava)
        .fee(1000000) // Customize fee
        .outputs(out1)
        .sendChangeTo(participantAddress.getErgoAddress)
        .build()

      val prover = ctx.newProverBuilder()
        .withDLogSecret(participantSecret)
        .build()

      val signedTx = prover.sign(tx)

      println(signedTx.toJson(false)) // Outputs the signed transaction
    }
  }

  def main(args: Array[String]): Unit = {
    val ergoClient = new ErgoClient() // Instantiate your Ergo client
    ergoClient.execute((ctx: BlockchainContext) => {
      println(ctx.getHeight)
      makeRaffle(ctx)
    })
  }
}
