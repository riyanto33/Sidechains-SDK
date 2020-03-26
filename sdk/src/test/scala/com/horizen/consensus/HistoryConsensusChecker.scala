package com.horizen.consensus

import java.util.Random

import com.horizen.SidechainHistory
import com.horizen.block.SidechainBlock
import com.horizen.companion.SidechainTransactionsCompanion
import com.horizen.fixtures.CompanionsFixture
import com.horizen.fixtures.sidechainblock.generation.{FinishedEpochInfo, GenerationRules, SidechainBlocksGenerator}
import com.horizen.params.NetworkParams
import com.horizen.storage.{InMemoryStorageAdapter, SidechainHistoryStorage}
import com.horizen.validation.ConsensusValidator

import scala.annotation.tailrec
import scala.collection.mutable
import scala.util.{Failure, Success}

trait HistoryConsensusChecker extends CompanionsFixture {
  def createHistory(params: NetworkParams, genesisBlock: SidechainBlock, finishedEpochInfo: FinishedEpochInfo): SidechainHistory = {
    val companion: SidechainTransactionsCompanion = getDefaultTransactionsCompanion

    val sidechainHistoryStorage: SidechainHistoryStorage = new SidechainHistoryStorage(new InMemoryStorageAdapter(), companion, params)
    SidechainHistory
      .genesisHistory(
        sidechainHistoryStorage,
        new ConsensusDataStorage(new InMemoryStorageAdapter()),
        params,
        genesisBlock,
        Seq(),
        Seq(new ConsensusValidator()),
        finishedEpochInfo.stakeConsensusEpochInfo)
      .get
  }

  def generateBlock(generationRule: GenerationRules, generator: SidechainBlocksGenerator, history: SidechainHistory): (Seq[SidechainBlocksGenerator], SidechainBlock) = {
    val (nextGenerator, generationResult) = generator.tryToGenerateCorrectBlock(generationRule)
    generationResult match {
      case Right(generatedBlockInfo) => (Seq(nextGenerator), generatedBlockInfo.block)
      case Left(finishedEpochInfo) => {
        val nonce = history.calculateNonceForEpoch(blockIdToEpochId(nextGenerator.lastBlockId))
        val stake = finishedEpochInfo.stakeConsensusEpochInfo
        history.applyFullConsensusInfo(nextGenerator.lastBlockId, FullConsensusEpochInfo(stake, nonce))
        println(s"//////////////// Epoch ${nextGenerator.lastBlockId} had been ended ////////////////")
        val (nextNextGenerator, firstEpochBlockGenerationInfo) = nextGenerator.tryToGenerateCorrectBlock(generationRule.copy(mcReferenceIsPresent = Some(true)))
        (Seq(nextNextGenerator), firstEpochBlockGenerationInfo.right.get.block)
      }
    }
  }

  def historyUpdateShallBeSuccessful(history: SidechainHistory, newBlock: SidechainBlock): SidechainHistory = {
    println(s"append to History: ${newBlock.id}")
    history.append(newBlock) match {
      case Failure(ex) => {
        println(s"Got exception during add block ${newBlock.id} to the history: ${ex.getMessage}")
        throw ex
      }
      case Success(historyWithBlock) => historyWithBlock._1
    }
  }

  def historyUpdateShallBeFailed(history: SidechainHistory, newBlock: SidechainBlock, incorrectGenerationRules: GenerationRules): Unit = {
    history.append(newBlock) match {
      case Failure(ex) => {
        println(s"Got expected exception during add block ${newBlock.id} to the history: ${ex.getMessage}")
      }
      case Success(historyWithBlock) => {
        println("Test had been failed")
        throw new IllegalStateException(s"Incorrect block generated by ${incorrectGenerationRules} had been successfully added")
      }
    }
  }

  def generatorSelection(rnd: Random, generators: mutable.IndexedSeq[SidechainBlocksGenerator]): SidechainBlocksGenerator = {
    @tailrec
    def generatorSelectionIteration(rnd: Random, index: Int, generators: mutable.IndexedSeq[SidechainBlocksGenerator]): SidechainBlocksGenerator = {
      if (index == 0) {
        generators(0)
      }
      else {
        if (rnd.nextBoolean()) {
          println()
          println(s"return ${index} generator from ${generators.size - 1}")
          generators(index)
        }
        else {
          generatorSelectionIteration(rnd, index - 1, generators)
        }
      }
    }
    generatorSelectionIteration(rnd, generators.size - 1, generators)
  }
}
