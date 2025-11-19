package com.lynus.cs203.services;

import com.lynus.cs203.blockchain.TariffAuditLog;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.*;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("BlockchainAuditService Test")
public class BlockchainAuditServiceTest {

    // Helper to compute expected hash using same canonical serialization that the service uses
    private static String computeExpectedHash(String... rows) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        for (String r : rows) {
            digest.update(r.getBytes(StandardCharsets.UTF_8));
        }
        byte[] finalHash = digest.digest();
        return "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(finalHash);
    }

    // Helper: read private static String field from BlockchainAuditService
    private static String getServiceStaticString(String name) throws Exception {
        Field f = BlockchainAuditService.class.getDeclaredField(name);
        f.setAccessible(true);
        return (String) f.get(null);
    }

    @Test
    public void hashTariffTable_handlesNullsAndValues_and_usesDefaultFetchSize() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);

        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2"); // non-mysql branch -> should set positive fetch size
        when(conn.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        // two rows
        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("trade_id")).thenReturn(1L, 2L);
        when(rs.getObject("product_id", Long.class)).thenReturn(10L, null);
        when(rs.getObject("country_id", Long.class)).thenReturn(100L, null);
        when(rs.getBigDecimal("tariff_rate")).thenReturn(new BigDecimal("1.50"), null);

        String hash = BlockchainAuditService.hashTariffTable(conn);
        assertNotNull(hash);
        assertTrue(hash.startsWith("0x"));

        String row1 = "1|10|100|" + new BigDecimal("1.50").stripTrailingZeros().toPlainString();
        String row2 = "2|NULL|NULL|NULL";
        String expected = computeExpectedHash(row1, row2);
        assertEquals(expected, hash);

        // verify the non-MySQL branch set the positive fetch size
        verify(ps).setFetchSize(1000);
    }

    @Test
    public void hashTariffTable_usesMySQLStreaming_fetchSizeMinValue() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);

        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MySQL");
        when(conn.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        // no rows required; we only want to verify fetch-size selection
        when(rs.next()).thenReturn(false);

        BlockchainAuditService.hashTariffTable(conn);

        // verify the MySQL streaming sentinel was used
        verify(ps).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void hashTariffTable_usesMariaDBStreaming_fetchSizeMinValue() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);

        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("MariaDB");
        when(conn.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(false);

        BlockchainAuditService.hashTariffTable(conn);

        verify(ps).setFetchSize(Integer.MIN_VALUE);
    }

    @Test
    public void audit_returnsIntegrityOk_whenHashesMatch() throws Exception {
        // prepare mocked Connection and resultset (two rows)
        Connection conn = mock(Connection.class);
        when(conn.isValid(anyInt())).thenReturn(true);

        PreparedStatement ping = mock(PreparedStatement.class);
        ResultSet pingRs = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT 1"))).thenReturn(ping);
        when(ping.executeQuery()).thenReturn(pingRs);

        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(conn.prepareStatement(startsWith("SELECT trade_id"), anyInt(), anyInt())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, true, false);
        when(rs.getLong("trade_id")).thenReturn(1L, 2L);
        when(rs.getObject("product_id", Long.class)).thenReturn(10L, null);
        when(rs.getObject("country_id", Long.class)).thenReturn(100L, null);
        when(rs.getBigDecimal("tariff_rate")).thenReturn(new BigDecimal("1.50"), null);

        String row1 = "1|10|100|" + new BigDecimal("1.50").stripTrailingZeros().toPlainString();
        String row2 = "2|NULL|NULL|NULL";
        String expectedLocalHash = computeExpectedHash(row1, row2);

        // stub DriverManager.getConnection using the exact args the service uses (read via reflection)
        String url = getServiceStaticString("LIVE_DB_URL");
        String user = getServiceStaticString("LIVE_DB_USERNAME");
        String pass = getServiceStaticString("LIVE_DB_PASSWORD");

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(url, user, pass)).thenReturn(conn);

            // mock Web3j.build to return a mock web3j
            Web3j web3j = mock(Web3j.class);
            try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                // mock contract and its remote function call
                TariffAuditLog contract = mock(TariffAuditLog.class);
                @SuppressWarnings("unchecked")
                RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                when(rfc.send()).thenReturn(expectedLocalHash);
                when(contract.getLatestHash()).thenReturn(rfc);

                // stub TariffAuditLog.load using the exact contract address the service uses
                String contractAddr = getServiceStaticString("CONTRACT_ADDRESS");
                try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                    talStatic.when(() -> TariffAuditLog.load(eq(contractAddr), any(Web3j.class), any(TransactionManager.class), any(ContractGasProvider.class)))
                            .thenReturn(contract);

                    BlockchainAuditService svc = new BlockchainAuditService();
                    var resp = svc.audit();

                    assertFalse(resp.isError());
                    assertTrue(resp.isIntegrityOk());
                    assertEquals(expectedLocalHash, resp.getLocalHash());
                    assertEquals(expectedLocalHash, resp.getOnChainHash());
                }
            }
        }
    }

    @Test
    public void audit_returnsMismatch_whenHashesDiffer() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.isValid(anyInt())).thenReturn(true);

        PreparedStatement ping = mock(PreparedStatement.class);
        ResultSet pingRs = mock(ResultSet.class);
        when(conn.prepareStatement(eq("SELECT 1"))).thenReturn(ping);
        when(ping.executeQuery()).thenReturn(pingRs);

        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);
        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
        when(conn.prepareStatement(startsWith("SELECT trade_id"), anyInt(), anyInt())).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);

        when(rs.next()).thenReturn(true, false);
        when(rs.getLong("trade_id")).thenReturn(1L);
        when(rs.getObject("product_id", Long.class)).thenReturn(null);
        when(rs.getObject("country_id", Long.class)).thenReturn(null);
        when(rs.getBigDecimal("tariff_rate")).thenReturn(null);

        String row1 = "1|NULL|NULL|NULL";
        String expectedLocalHash = computeExpectedHash(row1);

        String url = getServiceStaticString("LIVE_DB_URL");
        String user = getServiceStaticString("LIVE_DB_USERNAME");
        String pass = getServiceStaticString("LIVE_DB_PASSWORD");

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(url, user, pass)).thenReturn(conn);

            Web3j web3j = mock(Web3j.class);
            try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                TariffAuditLog contract = mock(TariffAuditLog.class);
                @SuppressWarnings("unchecked")
                RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                when(rfc.send()).thenReturn("0xdifferent");
                when(contract.getLatestHash()).thenReturn(rfc);

                String contractAddr = getServiceStaticString("CONTRACT_ADDRESS");
                try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                    talStatic.when(() -> TariffAuditLog.load(eq(contractAddr), any(Web3j.class), any(TransactionManager.class), any(ContractGasProvider.class)))
                            .thenReturn(contract);

                    BlockchainAuditService svc = new BlockchainAuditService();
                    var resp = svc.audit();

                    assertFalse(resp.isError());
                    assertFalse(resp.isIntegrityOk());
                    assertEquals(expectedLocalHash, resp.getLocalHash());
                    assertEquals("0xdifferent", resp.getOnChainHash());
                }
            }
        }
    }

    @Test
    public void audit_returnsError_whenConnectionValidationFails() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.isValid(anyInt())).thenReturn(false);

        String url = getServiceStaticString("LIVE_DB_URL");
        String user = getServiceStaticString("LIVE_DB_USERNAME");
        String pass = getServiceStaticString("LIVE_DB_PASSWORD");

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection(url, user, pass)).thenReturn(conn);

            BlockchainAuditService svc = new BlockchainAuditService();
            var resp = svc.audit();

            assertTrue(resp.isError());
            assertFalse(resp.isIntegrityOk());
            assertTrue(resp.getMessage().contains("Connection validation failed"));
        }
    }

    @Test
    public void main_runsAndPrintsVerified_whenHashesMatch() throws Exception {
        // capture stdout
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Connection conn = mock(Connection.class);
            doNothing().when(conn).setAutoCommit(false);
            doNothing().when(conn).setAutoCommit(true);

            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            DatabaseMetaData md = mock(DatabaseMetaData.class);
            when(conn.getMetaData()).thenReturn(md);
            when(md.getDatabaseProductName()).thenReturn("H2");
            when(conn.prepareStatement(startsWith("SELECT trade_id"), anyInt(), anyInt())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("trade_id")).thenReturn(1L);
            when(rs.getObject("product_id", Long.class)).thenReturn(null);
            when(rs.getObject("country_id", Long.class)).thenReturn(null);
            when(rs.getBigDecimal("tariff_rate")).thenReturn(null);

            String row1 = "1|NULL|NULL|NULL";
            String expectedLocalHash = computeExpectedHash(row1);

            String url = getServiceStaticString("LIVE_DB_URL");
            String user = getServiceStaticString("LIVE_DB_USERNAME");
            String pass = getServiceStaticString("LIVE_DB_PASSWORD");

            try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
                dm.when(() -> DriverManager.getConnection(url, user, pass)).thenReturn(conn);

                Web3j web3j = mock(Web3j.class);
                doNothing().when(web3j).shutdown();
                try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                    web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                    TariffAuditLog contract = mock(TariffAuditLog.class);
                    @SuppressWarnings("unchecked")
                    RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                    when(rfc.send()).thenReturn(expectedLocalHash);
                    when(contract.getLatestHash()).thenReturn(rfc);

                    String contractAddr = getServiceStaticString("CONTRACT_ADDRESS");
                    try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                        talStatic.when(() -> TariffAuditLog.load(eq(contractAddr), any(Web3j.class), any(TransactionManager.class), any(ContractGasProvider.class)))
                                .thenReturn(contract);

                        // call main - it prints to stdout which we captured
                        BlockchainAuditService.main(new String[]{});
                    }
                }
            }

            System.out.flush();
            String output = baos.toString("UTF-8");
            assertTrue(output.contains("✅ Database integrity verified!"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void main_printsMismatch_whenHashesDiffer() throws Exception {
        // capture stdout
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Connection conn = mock(Connection.class);
            doNothing().when(conn).setAutoCommit(false);

            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            DatabaseMetaData md = mock(DatabaseMetaData.class);
            when(conn.getMetaData()).thenReturn(md);
            when(md.getDatabaseProductName()).thenReturn("H2");
            when(conn.prepareStatement(startsWith("SELECT trade_id"), anyInt(), anyInt())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);

            // one row producing canonical "1|NULL|NULL|NULL"
            when(rs.next()).thenReturn(true, false);
            when(rs.getLong("trade_id")).thenReturn(1L);
            when(rs.getObject("product_id", Long.class)).thenReturn(null);
            when(rs.getObject("country_id", Long.class)).thenReturn(null);
            when(rs.getBigDecimal("tariff_rate")).thenReturn(null);

            String url = getServiceStaticString("LIVE_DB_URL");
            String user = getServiceStaticString("LIVE_DB_USERNAME");
            String pass = getServiceStaticString("LIVE_DB_PASSWORD");

            try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
                dm.when(() -> DriverManager.getConnection(url, user, pass)).thenReturn(conn);

                Web3j web3j = mock(Web3j.class);
                doNothing().when(web3j).shutdown();
                try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                    web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                    TariffAuditLog contract = mock(TariffAuditLog.class);
                    @SuppressWarnings("unchecked")
                    RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                    when(rfc.send()).thenReturn("0xdifferent");
                    when(contract.getLatestHash()).thenReturn(rfc);

                    String contractAddr = getServiceStaticString("CONTRACT_ADDRESS");
                    try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                        talStatic.when(() -> TariffAuditLog.load(eq(contractAddr), any(Web3j.class), any(TransactionManager.class), any(ContractGasProvider.class)))
                                .thenReturn(contract);

                        // call main - it prints to stdout which we captured
                        BlockchainAuditService.main(new String[]{});
                    }
                }
            }

            System.out.flush();
            String output = baos.toString("UTF-8");
            assertTrue(output.contains("❌ Database hash mismatch!"));
        } finally {
            System.setOut(originalOut);
        }
    }
}
