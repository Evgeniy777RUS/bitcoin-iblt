package se.rosenbaum.bitcoiniblt;

import org.bitcoinj.core.Block;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.params.MainNetParams;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.EasyMock;
import org.junit.Before;
import se.rosenbaum.bitcoiniblt.bytearraydata.ByteArrayDataTransactionCoder;
import se.rosenbaum.bitcoiniblt.bytearraydata.IBLTUtils;
import se.rosenbaum.bitcoiniblt.printer.FailureProbabilityPrinter;
import se.rosenbaum.bitcoiniblt.util.AggregateResultStats;
import se.rosenbaum.bitcoiniblt.util.BlockStatsResult;
import se.rosenbaum.bitcoiniblt.util.TestConfig;
import se.rosenbaum.bitcoiniblt.util.TransactionSets;
import se.rosenbaum.iblt.IBLT;

import java.io.IOException;
import java.util.List;

import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class BlockStatsClientCoderTest extends ClientCoderTest {
    protected TransactionSorter sorter;
    private BlockCoder sut;


    @Before
    public void setupBlockStatsClientCoderTest() {
        sorter = new CanonicalOrderTransactionSorter();
    }

    private void createBlockCoder(TestConfig config) {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(getParams(), config.getSalt(), config.getKeySize(),
                config.getValueSize());
        IBLT iblt = new IBLTUtils().createIblt(config.getCellCount(), config.getHashFunctionCount(), config.getKeySize(),
                config.getValueSize(), config.getKeyHashSize());
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    private void createBlockCoder(TestConfig config, IBLT iblt) {
        TransactionCoder transactionCoder = new ByteArrayDataTransactionCoder(getParams(), config.getSalt(), config.getKeySize(),
                config.getValueSize());
        sut = new BlockCoder(iblt, transactionCoder, sorter);
    }

    public BlockStatsResult testBlockStats(TestConfig config) throws IOException {
        createBlockCoder(config);
        TransactionSets sets = config.createTransactionSets();

        List<Transaction> sortedBlockTransactions = sorter.sort(sets.getSendersTransactions());

        Block myBlock = EasyMock.createMock(Block.class);
        expect(myBlock.getTransactions()).andReturn(sortedBlockTransactions);

        Block recreatedBlock = EasyMock.createMock(Block.class);
        Capture<Transaction> capture = new Capture<Transaction>(CaptureType.ALL);
        recreatedBlock.addTransaction(EasyMock.capture(capture));
        expectLastCall().anyTimes();

        replay(recreatedBlock, myBlock);

        BlockStatsResult result = new BlockStatsResult();

        long startTime = System.currentTimeMillis();
        IBLT iblt = sut.encode(myBlock);
        result.setEncodingTime(System.currentTimeMillis() - startTime);
        result.setTotalKeysCount(sut.getEncodedEntriesCount());

        createBlockCoder(config, iblt);

        startTime = System.currentTimeMillis();
        Block resultBlock = sut.decode(recreatedBlock, sets.getReceiversTransactions());
        result.setDecodingTime(System.currentTimeMillis() - startTime);
        result.setResidualKeysCount(sut.getResidualEntriesCount());

        if (resultBlock == null) {
            result.setSuccess(false);
            return result;
        }

        List<Transaction> decodedTransaction = capture.getValues();
        if (config.assertTransactionListCorrect()) {
            assertListsEqual(sortedBlockTransactions, decodedTransaction);
        }
        result.setSuccess(isListsEqual(sortedBlockTransactions, decodedTransaction));
        return result;
    }

    private boolean isListsEqual(List<Transaction> sortedBlockTransactions, List<Transaction> decodedTransaction) {
        if (sortedBlockTransactions == null && decodedTransaction == null) {
            return true;
        }
        if (sortedBlockTransactions == null || decodedTransaction == null) {
            return false;
        }
        if (sortedBlockTransactions.size() != decodedTransaction.size()) {
            return false;
        }
        for (int i = 0; i < sortedBlockTransactions.size(); i++) {
            if (!sortedBlockTransactions.get(i).equals(decodedTransaction.get(i))) {
                return false;
            }
        }
        return true;
    }

    private void assertListsEqual(List expected, List actual) {
        if (expected == null && actual == null) {
            return;
        }
        assertNotNull(expected);
        assertNotNull(actual);
        assertEquals(expected.size(), actual.size());
        assertArrayEquals(expected.toArray(new Object[0]), actual.toArray(new Object[0]));
    }

    public NetworkParameters getParams() {
        return MainNetParams.get();
    }

    protected AggregateResultStats testFailureProbability(FailureProbabilityPrinter printer, TestConfig config, int sampleCount) throws IOException {
        AggregateResultStats stats = new AggregateResultStats();

        for (int i = 0; i < sampleCount; i++) {
            BlockStatsResult result = testBlockStats(config);
            if (result == null) {
                break;
            }
            stats.addSample(result);
            if (printer != null && i % 100 == 0 && i > 0) {
                printer.logResult(config, stats);
            }
        }
        return stats;
    }
}
