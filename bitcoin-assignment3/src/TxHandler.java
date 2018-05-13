import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class TxHandler {


    private UTXOPool uTxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        uTxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        boolean invalid = false;

        UTXO[] utxoList = new UTXO[tx.numInputs()];
        for(int i=0; i<tx.numInputs(); ++i){
            Transaction.Input input = tx.getInput(i);
            UTXO utxo  =new UTXO(input.prevTxHash, input.outputIndex);
            Transaction.Output out = uTxoPool.getTxOutput(utxo);

            if(out == null) invalid=true;
            else utxoList[i] = utxo;
        }

        if(invalid) return false;

        for(int i=0; i<utxoList.length; ++i){
            Transaction.Output out = uTxoPool.getTxOutput(utxoList[i]);
            PublicKey address = out.address;
            byte[] signature = tx.getInput(i).signature;
            byte[] message = tx.getRawDataToSign(i);

            boolean result = Crypto.verifySignature(address, message, signature);
            if(!result) invalid = true;
        }

        if(invalid) return false;

        for(int i=0; i<utxoList.length; ++i){
            for(int j=i+1; j<utxoList.length; ++j)
                    if(utxoList[i].compareTo(utxoList[j]) == 0)
                        invalid = true;
        }

        if(invalid) return false;

        for(int i=0; i<tx.numOutputs(); ++i){
            Transaction.Output output = tx.getOutput(i);

            if(output.value < 0)
                invalid = true;
        }

        if(invalid) return false;

        double inputValue, outputValue;
        inputValue = 0;
        for(int i=0; i<tx.numInputs(); ++i){
            Transaction.Output out = uTxoPool.getTxOutput(utxoList[i]);
            inputValue += out.value;
        }

        outputValue = 0;
        for(int j=0; j<tx.numOutputs(); ++j){
            outputValue += tx.getOutput(j).value;
        }

        if(outputValue > inputValue)
            invalid = true;

        if(invalid) return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        //double spent
        //unorder transaction
        //complex transcation

        List<Transaction> result = new ArrayList<Transaction>();

        while(true){
            int acceptedTx = 0;
            for(Transaction tx : possibleTxs){
                if(result.contains(tx)) continue;

                boolean invalid = false;

                if(!isValidTx(tx)) invalid = true;

                if(invalid) continue;

                result.add(tx);
                //add unspent coins
                for(int i=0; i<tx.numOutputs(); ++i) {
                    UTXO utxo = new UTXO(tx.getHash(), i);
                    uTxoPool.addUTXO(utxo, tx.getOutput(i));
                }
                //remove spent coins
                for(int i=0; i<tx.numInputs(); ++i){
                    Transaction.Input input = tx.getInput(i);
                    UTXO utxo = new UTXO(input.prevTxHash, input.outputIndex);
                    uTxoPool.removeUTXO(utxo);
                }

                acceptedTx++;
            }

            if(acceptedTx == 0)
                break;
        }

        return result.toArray(new Transaction[0]);
    }

}
