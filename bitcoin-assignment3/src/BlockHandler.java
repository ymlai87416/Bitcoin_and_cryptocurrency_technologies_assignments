
import com.sun.crypto.provider.BlowfishKeyGenerator;

import java.security.*;

public class BlockHandler {
    private BlockChain blockChain;

    /** assume blockChain has the genesis block */
    public BlockHandler(BlockChain blockChain) {
        this.blockChain = blockChain;
    }

    /**
     * add {@code block} to the block chain if it is valid.
     * 
     * @return true if the block is valid and has been added, false otherwise
     */
    public boolean processBlock(Block block) {
        if (block == null)
            return false;
        return blockChain.addBlock(block);
    }

    /** create a new {@code block} over the max height {@code block} */
    public Block createBlock(PublicKey myAddress) {
        Block parent = blockChain.getMaxHeightBlock();
        byte[] parentHash = parent.getHash();
        Block current = new Block(parentHash, myAddress);
        UTXOPool uPool = blockChain.getMaxHeightUTXOPool();
        TransactionPool txPool = blockChain.getTransactionPool();
        TxHandler handler = new TxHandler(uPool);
        Transaction[] txs = txPool.getTransactions().toArray(new Transaction[0]);
        Transaction[] rTxs = handler.handleTxs(txs);
        for (int i = 0; i < rTxs.length; i++)
            current.addTransaction(rTxs[i]);

        current.finalize();
        if (blockChain.addBlock(current))
            return current;
        else
            return null;
    }

    /** process a {@code Transaction} */
    public void processTx(Transaction tx) {
        blockChain.addTransaction(tx);
    }


    public static PrivateKey alicePriv;
    public static PublicKey alicePub;

    public static PrivateKey bobPriv;
    public static PublicKey bobPub;

    public static PrivateKey minerPriv;
    public static PublicKey minerPub;

    static{
        try{
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
            SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
            keyGen.initialize(1024, random);

            KeyPair pair = keyGen.generateKeyPair();
            alicePriv = pair.getPrivate();
            alicePub = pair.getPublic();

            pair = keyGen.generateKeyPair();
            bobPriv = pair.getPrivate();
            bobPub = pair.getPublic();

            pair = keyGen.generateKeyPair();
            minerPriv = pair.getPrivate();
            minerPub = pair.getPublic();
        }
        catch(Exception ex){
            System.out.println("Error in init.");
        }

    }

    public static void main(String[] args){
        try{

            byte[] a = new byte[]{1,2,3};
            byte[] b = a.clone();
            System.out.println("Test 1" + (a==b));

            Block newBlock;

            Block gblock = new Block(null, minerPub);
            Transaction gTrans = new Transaction();
            gTrans.addOutput(100, alicePub);
            gTrans.finalize();
            gblock.addTransaction(gTrans);
            gblock.finalize();

            BlockChain bchain = new BlockChain(gblock);

            BlockHandler handler = new BlockHandler(bchain);

            //alice paid 100 btc to bob
            Transaction tx = new Transaction();
            tx.addInput(gTrans.getHash().clone(), 0);
            tx.addOutput(100, bobPub);
            tx.finalize();
            byte[] aliceSign = Crypto.generateSignature(alicePriv, tx.getRawDataToSign(0));
            tx.addSignature(aliceSign, 0);

            handler.processTx(tx);
            //Block newBlock = handler.createBlock(minerPub);
            //handler.processBlock(newBlock);

            //miner paid 10 btc to bob
            Transaction tx2 = new Transaction();
            tx2.addInput(gblock.getCoinbase().getHash().clone(), 0);
            tx2.addOutput(10, bobPub);
            tx2.addOutput(gblock.getCoinbase().getOutput(0).value-10, minerPub);
            tx2.finalize();
            byte[] minerSign = Crypto.generateSignature(minerPriv, tx2.getRawDataToSign(0));
            tx2.addSignature(minerSign, 0);

            handler.processTx(tx2);

            newBlock = handler.createBlock(minerPub);
            //handler.processBlock(newBlock);

            Block maxBlock = bchain.getMaxHeightBlock();
            UTXOPool maxPool = bchain.getMaxHeightUTXOPool();
            System.out.println("OK");
        }
        catch(Exception ex){
            System.out.println("Error");
            ex.printStackTrace();
        }

    }

    private static byte[] generateHash(byte[] message){
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        byte[] hash = digest.digest(message);

        return hash;
    }
}
