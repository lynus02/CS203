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
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.*;

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
            digest.update(r.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        byte[] finalHash = digest.digest();
        return "0x" + org.web3j.utils.Numeric.toHexStringNoPrefix(finalHash);
    }

    @Test
    public void hashTariffTable_handlesNullsAndValues() throws Exception {
        Connection conn = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        DatabaseMetaData md = mock(DatabaseMetaData.class);

        when(conn.getMetaData()).thenReturn(md);
        when(md.getDatabaseProductName()).thenReturn("H2");
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
    }

    @Test
    public void main_runsWithoutThrowing_whenHashesMatch() throws Exception {
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

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null)).thenReturn(conn);

            Web3j web3j = mock(Web3j.class);
            doNothing().when(web3j).shutdown();
            try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                TariffAuditLog contract = mock(TariffAuditLog.class);
                @SuppressWarnings("unchecked")
                RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                when(rfc.send()).thenReturn(expectedLocalHash);
                when(contract.getLatestHash()).thenReturn(rfc);

                try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                    talStatic.when(() -> TariffAuditLog.load(eq((String) null), eq(web3j), any(TransactionManager.class), any(ContractGasProvider.class)))
                            .thenReturn(contract);

                    BlockchainAuditService.main(new String[0]);
                }
            }
        }
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
    public void main_printsMismatch_whenHashesDiffer() throws Exception {
        // capture stdout
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(baos));

        try {
            Connection conn = mock(Connection.class);
            // main() calls setAutoCommit(false)
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

            try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
                dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null)).thenReturn(conn);

                Web3j web3j = mock(Web3j.class);
                try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                    web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                    TariffAuditLog contract = mock(TariffAuditLog.class);
                    @SuppressWarnings("unchecked")
                    RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                    // return a different on-chain hash to force the mismatch branch in main()
                    when(rfc.send()).thenReturn("0xdifferent");
                    when(contract.getLatestHash()).thenReturn(rfc);

                    try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                        talStatic.when(() -> TariffAuditLog.load(eq((String) null), eq(web3j), any(TransactionManager.class), any(ContractGasProvider.class)))
                                .thenReturn(contract);

                        BlockchainAuditService.main(new String[0]);
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
        public void audit_returnsIntegrityOk_whenHashesMatch() throws Exception {
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

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null)).thenReturn(conn);

            Web3j web3j = mock(Web3j.class);
            try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                TariffAuditLog contract = mock(TariffAuditLog.class);
                @SuppressWarnings("unchecked")
                RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                when(rfc.send()).thenReturn(expectedLocalHash);
                when(contract.getLatestHash()).thenReturn(rfc);

                try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                    talStatic.when(() -> TariffAuditLog.load(eq((String) null), eq(web3j), any(TransactionManager.class), any(ContractGasProvider.class)))
                            .thenReturn(contract);

                    BlockchainAuditService svc = new BlockchainAuditService();
                    var resp = svc.audit();

                    assertTrue(resp.isIntegrityOk());
                    assertFalse(resp.isError());
                    assertEquals(expectedLocalHash, resp.getLocalHash());
                    assertEquals(expectedLocalHash, resp.getOnChainHash());
                }
            }
        }

        // also verify the non-mysql fetch-size was set during hash computation
        verify(ps).setFetchSize(1000);
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

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null)).thenReturn(conn);

            Web3j web3j = mock(Web3j.class);
            try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                TariffAuditLog contract = mock(TariffAuditLog.class);
                @SuppressWarnings("unchecked")
                RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                when(rfc.send()).thenReturn("0xdifferent");
                when(contract.getLatestHash()).thenReturn(rfc);

                try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                    talStatic.when(() -> TariffAuditLog.load(eq((String) null), eq(web3j), any(TransactionManager.class), any(ContractGasProvider.class)))
                            .thenReturn(contract);

                    BlockchainAuditService svc = new BlockchainAuditService();
                    var resp = svc.audit();

                    assertFalse(resp.isIntegrityOk());
                    assertFalse(resp.isError());
                    assertEquals(expectedLocalHash, resp.getLocalHash());
                    assertEquals("0xdifferent", resp.getOnChainHash());
                }
            }
        }
    }

    @Test
    public void main_runsAndPrintsVerified_whenHashesMatch() throws Exception {
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

            try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
                dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null)).thenReturn(conn);

                Web3j web3j = mock(Web3j.class);
                doNothing().when(web3j).shutdown();
                try (MockedStatic<Web3j> web3jStatic = mockStatic(Web3j.class)) {
                    web3jStatic.when(() -> Web3j.build(any(HttpService.class))).thenReturn(web3j);

                    TariffAuditLog contract = mock(TariffAuditLog.class);
                    @SuppressWarnings("unchecked")
                    RemoteFunctionCall<String> rfc = mock(RemoteFunctionCall.class);
                    when(rfc.send()).thenReturn(expectedLocalHash);
                    when(contract.getLatestHash()).thenReturn(rfc);

                    try (MockedStatic<TariffAuditLog> talStatic = mockStatic(TariffAuditLog.class)) {
                        talStatic.when(() -> TariffAuditLog.load(eq((String) null), eq(web3j), any(TransactionManager.class), any(ContractGasProvider.class)))
                                .thenReturn(contract);

                        BlockchainAuditService.main(new String[0]);
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
    public void audit_returnsError_whenConnectionValidationFails() throws Exception {
        Connection conn = mock(Connection.class);
        when(conn.isValid(anyInt())).thenReturn(false);

        try (MockedStatic<java.sql.DriverManager> dm = mockStatic(java.sql.DriverManager.class)) {
            // match the exact null-args usage in the service
            dm.when(() -> DriverManager.getConnection((String) null, (String) null, (String) null))
                    .thenReturn(conn);

            BlockchainAuditService svc = new BlockchainAuditService();
            var resp = svc.audit();

            assertTrue(resp.isError());
            assertFalse(resp.isIntegrityOk());
            assertTrue(resp.getMessage().contains("Connection validation failed"));
        }
    }

    @Test
    public void hashTariffTable_usesMySQL_and_MariaDB_fetchSizeMinValue() throws Exception {
        // MySQL
        {
            Connection conn = mock(Connection.class);
            PreparedStatement ps = mock(PreparedStatement.class);
            ResultSet rs = mock(ResultSet.class);
            DatabaseMetaData md = mock(DatabaseMetaData.class);

            when(conn.getMetaData()).thenReturn(md);
            when(md.getDatabaseProductName()).thenReturn("MySQL");
            when(conn.prepareStatement(anyString(), anyInt(), anyInt())).thenReturn(ps);
            when(ps.executeQuery()).thenReturn(rs);
            when(rs.next()).thenReturn(false);

            BlockchainAuditService.hashTariffTable(conn);
            verify(ps).setFetchSize(Integer.MIN_VALUE);
            reset(ps, rs, md, conn);
        }

        // MariaDB
        {
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
    }


}
