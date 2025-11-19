package com.lynus.cs203.blockchain;

import io.reactivex.Flowable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.TypeEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Numeric;
import org.web3j.abi.datatypes.Function;
import org.web3j.tuples.generated.Tuple3;

import java.lang.reflect.Field;
import java.math.BigInteger;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("TariffAuditLog Tests")
class TariffAuditLogTest {

    private final String ADDRESS = "0x1234567890123456789012345678901234567890";

    // Test-only subclass that makes the protected contract method public so Mockito can stub it.
    static class TestableTariffAuditLog extends TariffAuditLog {
        protected TestableTariffAuditLog(String contractAddress, Web3j web3j, Credentials credentials, ContractGasProvider contractGasProvider) {
            super(contractAddress, web3j, credentials, contractGasProvider);
        }

        // Override and make public; keep the same checked exception as in Contract (IOException).
        @Override
        public List<Type> executeCallMultipleValueReturn(Function function) throws java.io.IOException {
            return super.executeCallMultipleValueReturn(function);
        }
    }

    private Log buildHashStoredLog(String dbHashValue, String uploaderAddress, BigInteger timestamp) {
        Utf8String dbHash = new Utf8String(dbHashValue);
        Uint256 ts = new Uint256(timestamp);

        Function tmp = new Function("unused", List.of(dbHash, ts), Collections.emptyList());
        String encodedWithSelector = FunctionEncoder.encode(tmp);
        String data = "0x" + encodedWithSelector.substring(10);

        Address uploader = new Address(uploaderAddress);
        String uploaderTopic = Numeric.prependHexPrefix(TypeEncoder.encode(uploader));

        String eventSignature = EventEncoder.encode(TariffAuditLog.HASHSTORED_EVENT);

        Log log = new Log();
        log.setTopics(List.of(eventSignature, uploaderTopic));
        log.setData(data);
        log.setAddress("0x0000000000000000000000000000000000000000");
        return log;
    }

    @Test
    @DisplayName("getHashStoredEvents parses TransactionReceipt logs correctly")
    void testGetHashStoredEvents_parsesReceipt() {
        String dbHash = "0xdeadbeef";
        BigInteger ts = BigInteger.valueOf(123456789L);
        Log log = buildHashStoredLog(dbHash, ADDRESS, ts);

        TransactionReceipt receipt = new TransactionReceipt();
        receipt.setLogs(List.of(log));

        List<TariffAuditLog.HashStoredEventResponse> events = TariffAuditLog.getHashStoredEvents(receipt);
        assertNotNull(events);
        assertEquals(1, events.size());

        TariffAuditLog.HashStoredEventResponse ev = events.get(0);
        assertEquals(dbHash, ev.dbHash);
        assertTrue(ev.uploader.toLowerCase().endsWith(ADDRESS.substring(2).toLowerCase()));
        assertEquals(ts, ev.timestamp);
    }

    @Test
    @DisplayName("getHashStoredEventFromLog parses a single Log correctly")
    void testGetHashStoredEventFromLog_parsesLog() {
        String dbHash = "0xfeedface";
        BigInteger ts = BigInteger.valueOf(42L);
        Log log = buildHashStoredLog(dbHash, ADDRESS, ts);

        TariffAuditLog.HashStoredEventResponse ev = TariffAuditLog.getHashStoredEventFromLog(log);
        assertNotNull(ev);
        assertEquals(dbHash, ev.dbHash);
        assertTrue(ev.uploader.toLowerCase().endsWith(ADDRESS.substring(2).toLowerCase()));
        assertEquals(ts, ev.timestamp);
    }

    @Test
    @DisplayName("hashStoredEventFlowable returns mapped event responses from web3j flowable")
    void testHashStoredEventFlowable_mapsFlowable() {
        String dbHash = "0xabad1dea";
        BigInteger ts = BigInteger.valueOf(999L);
        Log log = buildHashStoredLog(dbHash, ADDRESS, ts);

        Web3j web3j = mock(Web3j.class);
        when(web3j.ethLogFlowable(any(EthFilter.class))).thenReturn(Flowable.just(log));

        TransactionManager txManager = mock(TransactionManager.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);

        TariffAuditLog contract = TariffAuditLog.load("0x0000000000000000000000000000000000000000", web3j, txManager, gasProvider);

        TariffAuditLog.HashStoredEventResponse resp1 = contract.hashStoredEventFlowable(new EthFilter()).blockingFirst();
        assertNotNull(resp1);
        assertEquals(dbHash, resp1.dbHash);
        assertEquals(ts, resp1.timestamp);

        TariffAuditLog.HashStoredEventResponse resp2 = contract.hashStoredEventFlowable(DefaultBlockParameterName.EARLIEST, DefaultBlockParameterName.LATEST).blockingFirst();
        assertNotNull(resp2);
        assertEquals(dbHash, resp2.dbHash);
        assertEquals(ts, resp2.timestamp);

        verify(web3j, atLeastOnce()).ethLogFlowable(any(EthFilter.class));
    }

    @Test
    @DisplayName("factory methods create RemoteCall objects without executing network")
    void testRemoteFunctionFactories() {
        Web3j web3j = mock(Web3j.class);
        TransactionManager txManager = mock(TransactionManager.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);

        TariffAuditLog contract = TariffAuditLog.load("0x0000000000000000000000000000000000000000", web3j, txManager, gasProvider);

        assertNotNull(contract.getLatestHash());
        assertNotNull(contract.logs(BigInteger.ONE));
        assertNotNull(contract.storeHash("0xabc"));
    }

    @Test
    @DisplayName("deprecated load and deploy overloads are callable")
    void testDeprecatedLoadsAndDeploys() {
        Web3j web3j = mock(Web3j.class);
        TransactionManager txManager = mock(TransactionManager.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);

        Credentials creds = Credentials.create("0123456789012345678901234567890123456789012345678901234567890123");
        BigInteger gasPrice = BigInteger.valueOf(1L);
        BigInteger gasLimit = BigInteger.valueOf(2L);

        TariffAuditLog c1 = TariffAuditLog.load("0x0000000000000000000000000000000000000000", web3j, creds, gasPrice, gasLimit);
        assertNotNull(c1);
        TariffAuditLog c2 = TariffAuditLog.load("0x0000000000000000000000000000000000000000", web3j, txManager, gasPrice, gasLimit);
        assertNotNull(c2);

        TariffAuditLog.linkLibraries(Collections.emptyList());

        assertNotNull(TariffAuditLog.deploy(web3j, creds, gasProvider));
        assertNotNull(TariffAuditLog.deploy(web3j, txManager, gasProvider));
        assertNotNull(TariffAuditLog.deploy(web3j, creds, gasPrice, gasLimit));
        assertNotNull(TariffAuditLog.deploy(web3j, txManager, gasPrice, gasLimit));
    }

    @Test
    @DisplayName("HashStoredEventResponse is a simple POJO")
    void testHashStoredEventResponse_pojo() {
        TariffAuditLog.HashStoredEventResponse r = new TariffAuditLog.HashStoredEventResponse();
        r.uploader = "0xabc";
        r.dbHash = "0x123";
        r.timestamp = BigInteger.TEN;

        assertEquals("0xabc", r.uploader);
        assertEquals("0x123", r.dbHash);
        assertEquals(BigInteger.TEN, r.timestamp);
    }

    @Test
    @DisplayName("logs RemoteFunctionCall callable returns Tuple3 via stubbed executeCallMultipleValueReturn")
    void testLogsRemoteFunctionCall_invokesCallable() throws Exception {
        Web3j web3j = mock(Web3j.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);
        Credentials creds = Credentials.create("0123456789012345678901234567890123456789012345678901234567890123");

        // instantiate the Testable subclass (so the overridden method is visible)
        TestableTariffAuditLog real = new TestableTariffAuditLog("0x0000000000000000000000000000000000000000", web3j, creds, gasProvider);
        TestableTariffAuditLog spy = spy(real);

        // prepare synthetic returned Types for the callable
        List<Type> results = List.of(
                new Utf8String("myHash"),
                new Uint256(BigInteger.valueOf(55L)),
                new Address(ADDRESS)
        );

        // stub the now-public overridden method on the subclass
        doReturn(results).when(spy).executeCallMultipleValueReturn(any(Function.class));

        Tuple3<String, BigInteger, String> tuple = spy.logs(BigInteger.ONE).send();
        assertNotNull(tuple);
        assertEquals("myHash", tuple.getValue1());
        assertEquals(BigInteger.valueOf(55L), tuple.getValue2());
        assertTrue(tuple.getValue3().toLowerCase().endsWith(ADDRESS.substring(2).toLowerCase()));
    }

    @Test
    @DisplayName("getDeploymentBinary uses BINARY when librariesLinkedBinary is null")
    void testGetDeploymentBinary_nullLibrariesUsesOriginalBinary() throws Exception {
        Web3j web3j = mock(Web3j.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);
        Credentials creds = Credentials.create("0123456789012345678901234567890123456789012345678901234567890123");

        Field f = TariffAuditLog.class.getDeclaredField("librariesLinkedBinary");
        f.setAccessible(true);
        f.set(null, null);

        assertNotNull(TariffAuditLog.deploy(web3j, creds, gasProvider));
    }

    @Test
    @DisplayName("non-deprecated load with Credentials and ContractGasProvider is callable")
    void testLoadWithCredentialsAndGasProvider() {
        Web3j web3j = mock(Web3j.class);
        ContractGasProvider gasProvider = mock(ContractGasProvider.class);
        Credentials creds = Credentials.create("0123456789012345678901234567890123456789012345678901234567890123");

        TariffAuditLog contract = TariffAuditLog.load("0x0000000000000000000000000000000000000000", web3j, creds, gasProvider);
        assertNotNull(contract);
    }
}
