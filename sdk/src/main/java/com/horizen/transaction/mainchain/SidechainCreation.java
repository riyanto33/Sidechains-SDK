package com.horizen.transaction.mainchain;

import com.google.common.primitives.Bytes;
import com.google.common.primitives.Ints;
import com.horizen.block.MainchainTxSidechainCreationCrosschainOutput;
import com.horizen.box.ForgerBox;
import com.horizen.box.data.ForgerBoxData;
import com.horizen.proposition.PublicKey25519Proposition;
import com.horizen.secret.PrivateKey25519;
import com.horizen.utils.BytesUtils;
import com.horizen.utils.Ed25519;
import com.horizen.utils.Utils;
import com.horizen.utils.Pair;
import com.horizen.vrf.VRFKeyGenerator;
import com.horizen.vrf.VRFPublicKey;
import com.horizen.vrf.VRFSecretKey;
import scala.Tuple2;

import java.util.Arrays;
import java.util.Optional;

public final class SidechainCreation implements SidechainRelatedMainchainOutput<ForgerBox> {

    private MainchainTxSidechainCreationCrosschainOutput output;
    private byte[] containingTxHash;
    private int index;

    public SidechainCreation(MainchainTxSidechainCreationCrosschainOutput output, byte[] containingTxHash, int index) {
        this.output = output;
        this.containingTxHash = containingTxHash;
        this.index = index;
    }
    @Override
    public byte[] hash() {
        return BytesUtils.reverseBytes(Utils.doubleSHA256Hash(Bytes.concat(
                BytesUtils.reverseBytes(output.hash()),
                BytesUtils.reverseBytes(containingTxHash),
                BytesUtils.reverseBytes(Ints.toByteArray(index))
        )));
    }

    @Override
    public Optional<ForgerBox> getBox() {
        // at the moment sc creation output doesn't create any new coins.
        return Optional.of(getHardcodedGenesisForgerBox());
    }

    @Override
    public byte[] bytes() {
        return Bytes.concat(
                output.sidechainCreationOutputBytes(),
                containingTxHash,
                Ints.toByteArray(index)
        );
    }

    public static SidechainCreation parseBytes(byte[] bytes) {
        if(bytes.length < 36 + MainchainTxSidechainCreationCrosschainOutput.SIDECHAIN_CREATION_OUTPUT_SIZE())
            throw new IllegalArgumentException("Input data corrupted.");

        int offset = 0;

        MainchainTxSidechainCreationCrosschainOutput output = MainchainTxSidechainCreationCrosschainOutput.create(bytes, offset).get();
        offset += MainchainTxSidechainCreationCrosschainOutput.SIDECHAIN_CREATION_OUTPUT_SIZE();

        byte[] txHash = Arrays.copyOfRange(bytes, offset, offset + 32);
        offset += 32;

        int idx = BytesUtils.getInt(bytes, offset);
        return new SidechainCreation(output, txHash, idx);
    }

    @Override
    public SidechainRelatedMainchainOutputSerializer serializer() {
        return SidechainCreationSerializer.getSerializer();
    }

    public int withdrawalEpochLength() {
        return output.withdrawalEpochLength();
    }



    private static Pair<byte[], byte[]> genesisStakeKeys = Ed25519.createKeyPair("ThatForgerBoxShallBeGetFromGenesisBoxNotHardcoded".getBytes());
    public static PrivateKey25519 genesisSecret = new PrivateKey25519(genesisStakeKeys.getKey(), genesisStakeKeys.getValue());
    public static Tuple2<VRFSecretKey, VRFPublicKey> genesisVrfPair = VRFKeyGenerator.generate(genesisStakeKeys.getKey());

    public static long initialValue = 10000000000L;
    public static ForgerBox getHardcodedGenesisForgerBox() {
        PublicKey25519Proposition proposition = genesisSecret.publicImage();
        PublicKey25519Proposition rewardProposition = genesisSecret.publicImage();
        VRFPublicKey vrfPubKey = genesisVrfPair._2;
        ForgerBoxData forgerBoxData = new ForgerBoxData(proposition, initialValue, rewardProposition, vrfPubKey);
        long nonce = 42L;

        return forgerBoxData.getBox(nonce);
    }

}
