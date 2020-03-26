package com.horizen.fixtures.sidechainblock.generation

import com.horizen.consensus.{ConsensusSlotNumber, NonceConsensusEpochInfo}
import com.horizen.utils.MerklePath
import com.horizen.vrf.VRFProof


case class PossibleForger(forgingData: SidechainForgingData,
                          merklePathInPreviousEpochOpt: Option[MerklePath],
                          merklePathInPrePreviousEpochOpt: Option[MerklePath],
                          spentInEpochsAgoOpt: Option[Int]) {
  require(!spentInEpochsAgoOpt.exists(_ > PossibleForger.boxToForgerBoxDelayInEpochs))

  def couldBePossibleForgerInNextEpoch: Boolean = spentInEpochsAgoOpt.filter(_ >= PossibleForger.boxToForgerBoxDelayInEpochs).isEmpty

  def createPossibleForgerForTheNextEpoch(newMerklePathOpt: Option[MerklePath]): PossibleForger = {
    copy(
      merklePathInPreviousEpochOpt = newMerklePathOpt,
      merklePathInPrePreviousEpochOpt = merklePathInPreviousEpochOpt,
      spentInEpochsAgoOpt = spentInEpochsAgoOpt.map(_ + 1))
  }

  def isNotSpent: Boolean = spentInEpochsAgoOpt.isEmpty

  def canBeForger(slotNumber: ConsensusSlotNumber, nonceConsensusEpochInfo: NonceConsensusEpochInfo, totalStake: Long, additionalCheck: Boolean => Boolean): Option[VRFProof] = {
    merklePathInPrePreviousEpochOpt.flatMap(_ => forgingData.canBeForger(slotNumber, nonceConsensusEpochInfo, totalStake, additionalCheck))
  }

  override def toString: String = {
    "PossibleForger: " + forgingData.toString
  }
}

object PossibleForger{
  val boxToForgerBoxDelayInEpochs: Int = 2 //0 -- current epoch, 1 -- previous epoch, 2 -- preprevious epoch
}
