package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Transaction;

import java.util.Collection;
import java.util.Map;

public interface TransactionCoder<K, V> {
    public Map<K, V> encodeTransaction(Transaction transaction);

    Collection<Transaction> decodeTransactions(Map<K, V> entries);
}
