// Block Chain should maintain only limited block nodes to satisfy the functions
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private List<BlockAux> recentBlocks;
    private TransactionPool globalPool;
    private boolean cacheValid = false;
    private BlockAux maxHeightBlockAux = null;

    /**
     * create an empty block chain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        recentBlocks = new ArrayList<>();

        BlockAux root = new BlockAux(genesisBlock, new Date().getTime(), 1);
        UTXOPool pool = new UTXOPool();

        updateUTXOPool(pool, genesisBlock.getTransactions());
        updateUTXOPool(pool, Arrays.asList(genesisBlock.getCoinbase()));

        root.setUTXOPool(pool);
        addBlockAux(root);

        globalPool = new TransactionPool();
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        BlockAux max= getMaxHeightBlockAux();

        return max.block;
    }

    private BlockAux getMaxHeightBlockAux(){
        //if(cacheValid)
        //    return maxHeightBlockAux;
        BlockAux max = recentBlocks.get(0);
        for(int i=1; i<recentBlocks.size(); ++i) {
            if (max.height < recentBlocks.get(i).height)
                max  = recentBlocks.get(i);
            else{
                if(max.height == recentBlocks.get(i).height &&
                        max.timestamp > recentBlocks.get(i).timestamp)
                    max = recentBlocks.get(i);
            }
        }
        maxHeightBlockAux = max;
        cacheValid = true;

        return max;
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        BlockAux max= getMaxHeightBlockAux();

        return max.pool;
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return globalPool;
    }

    /**
     * Add {@code block} to the block chain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}.
     * 
     * <p>
     * For example, you can try creating a new block over the genesis block (block height 2) if the
     * block chain height is {@code <=
     * CUT_OFF_AGE + 1}. As soon as {@code height > CUT_OFF_AGE + 1}, you cannot create a new block
     * at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if(block.getPrevBlockHash()==null)
            return false;

        BlockAux found = null;
        ByteArrayWrapper prevBlockHash = new ByteArrayWrapper(block.getPrevBlockHash());
        for(int i=0; i<recentBlocks.size(); ++i) {
            ByteArrayWrapper blockHash = new ByteArrayWrapper(recentBlocks.get(i).block.getHash());
            if (blockHash.equals(prevBlockHash)) {
            //if (recentBlocks.get(i).block.getHash() == block.getPrevBlockHash()) {
                found = recentBlocks.get(i);
                break;
            }
        }

        if(found==null) return false;

        //add the block to the recent list
        BlockAux blockAux = new BlockAux(block, new Date().getTime(), found.height+1);
        UTXOPool pool = new UTXOPool(found.getUTXOPool());

        TxHandler txHandler = new TxHandler(found.getUTXOPool());

        Transaction[] txList = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] validSet = txHandler.handleTxs(txList);

        //all transaction in a block should be valid
        if(txList.length != validSet.length)
            return false;

        updateUTXOPool(pool, block.getTransactions());
        updateUTXOPool(pool, Arrays.asList(block.getCoinbase()));

        blockAux.pool = pool;
        addBlockAux(blockAux);

        //remove the appeared transaction from the transaction pool so that other won't mine again.
        for(Transaction tx: block.getTransactions())
            globalPool.removeTransaction(tx.getHash());

        //housekeep the list
        houseKeepRecentBlocks();

        return true;
    }

    private void houseKeepRecentBlocks(){
        BlockAux aux = getMaxHeightBlockAux();

        ArrayList<BlockAux> pendingRemove = new ArrayList<>();
        for(int i=0; i<recentBlocks.size(); ++i){
            if(recentBlocks.get(i).height < aux.height - CUT_OFF_AGE)
                pendingRemove.add(recentBlocks.get(i));
        }

        for(int i=0; i<pendingRemove.size(); ++i)
            recentBlocks.remove(pendingRemove.get(i));

        cacheValid = false;
    }

    private void updateUTXOPool(UTXOPool pool, List<Transaction> transactions){
        for(Transaction tx: transactions){
            for(int i=0; i<tx.numOutputs(); ++i) {
                UTXO utxo = new UTXO(tx.getHash(), i);
                pool.addUTXO(utxo, tx.getOutput(i));
            }
            //remove spent coins
            for(int i=0; i<tx.numInputs(); ++i){
                Transaction.Input input = tx.getInput(i);
                UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                pool.removeUTXO(utxo);
            }
        }
    }

    private void addBlockAux(BlockAux aux){
        cacheValid = false;
        recentBlocks.add(aux);
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        globalPool.addTransaction(tx);
    }

    private class BlockAux{
        private Block block;
        private long timestamp;
        private int height;
        private UTXOPool pool;

        public BlockAux(Block block, long timestamp, int height){
            this.block = block;
            this.timestamp = timestamp;
            this.height = height;
        }

        private void setUTXOPool(UTXOPool pool){
            this.pool = pool;
        }

        private UTXOPool getUTXOPool(){
            return this.pool;
        }
    }


}