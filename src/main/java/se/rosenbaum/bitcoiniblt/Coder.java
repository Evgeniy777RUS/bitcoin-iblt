package se.rosenbaum.bitcoiniblt;

import com.google.bitcoin.core.NetworkParameters;
import com.google.bitcoin.core.Sha256Hash;
import com.google.bitcoin.core.Transaction;
import se.rosenbaum.iblt.data.LongData;

import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class Coder {
    NetworkParameters params;
    byte[] salt;

    public Coder(NetworkParameters params, byte[] salt) {
        this.params = params;
        this.salt = salt;
    }

    public Map<LongData, LongData> encodeTransaction(Transaction transaction) {
        Map<LongData, LongData> map = new HashMap<LongData, LongData>();

        assertEquals(256 / 8, salt.length);
        byte[] transactionId = transaction.getHash().getBytes();
        byte[] key = Arrays.copyOf(transactionId, transactionId.length + salt.length);
        for (int i = 0; i < salt.length; i++) {
            key[transactionId.length + i] = salt[i];
        }
        key = Sha256Hash.create(key).getBytes();

        byte[] keyBytes = Arrays.copyOfRange(key, 0, 8); // 64 first bits (the last to bytes will be overwritten by counter
        char keyCounter = 0; // char is a 16 bit unsigned integer

        byte[] bytes = transaction.bitcoinSerialize();
        for (int i = 0; i < bytes.length; i += 8) {
            byte[] keyCounterBytes = ByteBuffer.allocate(2).putChar(keyCounter++).array();
            keyBytes[6] = keyCounterBytes[0];
            keyBytes[7] = keyCounterBytes[1];

            LongData keyData = new LongData(ByteBuffer.wrap(keyBytes).getLong());
            LongData valueData = new LongData(ByteBuffer.wrap(Arrays.copyOfRange(bytes, i, i+8)).getLong());
            map.put(keyData, valueData);
        }
        return map;
    }

    public Transaction decodeTransaction(Map<LongData, LongData> map) {
        byte[] txBytes = new byte[map.size() * 8];

        for (Map.Entry<LongData, LongData> entry : map.entrySet()) {
            long key = entry.getKey().getValue();
            byte[] keyBytes = ByteBuffer.allocate(8).putLong(key).array();
            char keyCounter = ByteBuffer.wrap(keyBytes, 6, 2).getChar();

            long value = entry.getValue().getValue();
            byte[] valueBytes = ByteBuffer.allocate(8).putLong(value).array();

            for (int i = 0; i < 8; i++) {
                txBytes[keyCounter*8+i] = valueBytes[i];
            }
        }

        return new Transaction(params, txBytes);

    }

}
