package com.horizen.block

trait MainchainTxCrosschainOutput {
  val outputType: Byte
  val sidechainId: Array[Byte]
  val hash: Array[Byte]
}