import java.security.cert.CollectionCertStoreParameters;
import java.util.*;
import java.util.stream.Collectors;

/* CompliantNode refers to a node that follows the rules (not malicious)*/
public class CompliantNode implements Node {
    boolean[] trustable;

    double p_graph;
    double p_malicious;
    double p_txDistribution;
    int numRounds;

    Set<Transaction> pendingTransaction;

    public CompliantNode(double p_graph, double p_malicious, double p_txDistribution, int numRounds) {
        this.p_graph=p_graph;
        this.p_malicious=p_malicious;
        this.p_txDistribution=p_txDistribution;
        this.numRounds=numRounds;

        this.pendingTransaction = new HashSet<Transaction>();
    }

    public void setFollowees(boolean[] followees) {
        trustable = new boolean[followees.length];
        for(int i=0; i<followees.length; ++i)
            trustable[i] = followees[i];
    }

    public void setPendingTransaction(Set<Transaction> pendingTransactions) {
        // IMPLEMENT THIS
        this.pendingTransaction.addAll(pendingTransactions);
    }

    public Set<Transaction> sendToFollowers() {
        numRounds--;
        return pendingTransaction;
    }

    public void receiveFromFollowees(Set<Candidate> candidates) {
        // IMPLEMENT THIS
        Map<Integer, List<Transaction>> group = new TreeMap<>();
        for(Candidate candidate : candidates) {
            if (!group.containsKey(candidate.sender))
                group.put(candidate.sender, new ArrayList<>());

            group.get(candidate.sender).add(candidate.tx);
        }

        for(Integer sender: group.keySet()){
            for(Transaction tx: group.get(sender))
                pendingTransaction.add(tx);
        }
        //
    }
}
