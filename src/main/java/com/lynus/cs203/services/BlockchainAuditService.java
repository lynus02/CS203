package com.lynus.cs203.services;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.web3j.protocol.Web3j;
import org.web3j.tx.ClientTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.DefaultGasProvider;
import com.lynus.cs203.blockchain.TariffAuditLog;
import com.lynus.cs203.dtos.response.AuditResponse;
import org.web3j.utils.Numeric;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.sql.*;
import org.web3j.protocol.http.HttpService;
import java.nio.charset.StandardCharsets;

@Service
@AllArgsConstructor
public class BlockchainAuditService {

    private static final String LIVE_DB_URL = System.getenv("LIVE_DB_URL");
    private static final String LIVE_DB_USERNAME = System.getenv("LIVE_DB_USERNAME");
    private static final String LIVE_DB_PASSWORD = System.getenv("LIVE_DB_PASSWORD");

    private static final String RPC_URL = System.getenv("BLOCKCHAIN_RPC_URL");
    private static final String CONTRACT_ADDRESS = System.getenv("BLOCKCHAIN_CONTRACT_ADDRESS");

    public AuditResponse audit() {
        try {

            final String localHash;
            // Use DriverManager to always target the live DB
            try (Connection conn = DriverManager.getConnection(LIVE_DB_URL, LIVE_DB_USERNAME, LIVE_DB_PASSWORD)) {
                if (!conn.isValid(5)) {
                    throw new SQLException("Connection validation failed");
                }

                // Ensure each statement sees latest committed data
                conn.setAutoCommit(true);
                conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

                // Force a no-op query to ensure connection is fresh
                try (PreparedStatement ps = conn.prepareStatement("SELECT 1")) {
                    ps.executeQuery();
                }

                System.out.println("DEBUG: Using LIVE DB URL" + LIVE_DB_URL);
                System.out.println("Transaction isolation: " + conn.getTransactionIsolation());
                localHash = hashTariffTable(conn);
            }

            try (Web3j web3j = Web3j.build(new HttpService(RPC_URL))) {
                TransactionManager txManager = new ClientTransactionManager(web3j, "0x0000000000000000000000000000000000000000");
                TariffAuditLog contract = TariffAuditLog.load(CONTRACT_ADDRESS, web3j, txManager, new DefaultGasProvider());

                String onChainHash = contract.getLatestHash().send();

                System.out.println("Local tariff hash: " + localHash);
                System.out.println("On-chain tariff hash: " + onChainHash);

                boolean integrityOk = localHash != null && localHash.equalsIgnoreCase(onChainHash);
                String message = integrityOk ? "Database integrity verified" : "Database hash mismatch";
                return new AuditResponse(integrityOk, localHash, onChainHash, false, message);
            }
        } catch (Exception e) {
            return new AuditResponse(false, null, null, true, e.getMessage());
        }
    }

    // CLI entry left intact
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(LIVE_DB_URL, LIVE_DB_USERNAME, LIVE_DB_PASSWORD);
        conn.setAutoCommit(false);
        String localHash = hashTariffTable(conn);
        System.out.println("Local tariff hash: " + localHash);
        conn.close();

        Web3j web3j = Web3j.build(new HttpService(RPC_URL));
        TransactionManager txManager = new ClientTransactionManager(web3j, "0x0000000000000000000000000000000000000000");
        TariffAuditLog contract = TariffAuditLog.load(CONTRACT_ADDRESS, web3j, txManager, new DefaultGasProvider());

        String onChainHash = contract.getLatestHash().send();

        System.out.println("On-chain tariff hash: " + onChainHash);

        if (localHash.equalsIgnoreCase(onChainHash)) {
            System.out.println("✅ Database integrity verified!");
        } else {
            System.out.println("❌ Database hash mismatch!");
        }

        web3j.shutdown();
    }

    public static String hashTariffTable(Connection conn) throws Exception {
        // Ensure we read the latest committed data
        conn.setAutoCommit(true);
        conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);

        // Use deterministic ordering for consistent hashing
        String sql = "SELECT trade_id, product_id, country_id, tariff_rate " +
                "FROM tariff " +
                "ORDER BY trade_id ASC, product_id ASC, country_id ASC";

        try (PreparedStatement ps = conn.prepareStatement(
                sql,
                ResultSet.TYPE_FORWARD_ONLY,
                ResultSet.CONCUR_READ_ONLY)) {

            // Choose fetch-size behavior based on DB vendor to avoid invalid negative sentinel on H2 (used in testing)
            String dbProduct = conn.getMetaData().getDatabaseProductName();
            if (dbProduct != null && (dbProduct.toLowerCase().contains("mysql") || dbProduct.toLowerCase().contains("mariadb"))) {
                // MySQL/MariaDB: use streaming sentinel
                ps.setFetchSize(Integer.MIN_VALUE);
            } else {
                // Other DBs (H2, Postgres, etc.): use a reasonable positive fetch size or leave default
                ps.setFetchSize(1000); // or omit this line to use driver default
            }

            try (ResultSet rs = ps.executeQuery()) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                int rowCount = 0;

                while (rs.next()) {
                    rowCount++;

                    // Read values with explicit null handling
                    long tradeId = rs.getLong("trade_id");
                    Long productId = rs.getObject("product_id", Long.class);
                    Long countryId = rs.getObject("country_id", Long.class);
                    BigDecimal tariffRate = rs.getBigDecimal("tariff_rate");

                    // Serialize row data in a canonical format
                    String rowData = serializeRow(tradeId, productId, countryId, tariffRate);

                    // Update digest with row bytes
                    digest.update(rowData.getBytes(StandardCharsets.UTF_8));
                }

                // Finalize hash
                byte[] finalHash = digest.digest();
                String hexHash = "0x" + Numeric.toHexStringNoPrefix(finalHash);

                System.out.println("DEBUG: Hashed " + rowCount + " rows -> " + hexHash);
                return hexHash;
            }
        }
    }

    /**
     * Serializes a tariff row into a canonical string format.
     * Uses pipe delimiters and explicit NULL markers for consistency.
     *
     * @param tradeId The trade identifier (never null)
     * @param productId The product identifier (nullable)
     * @param countryId The country identifier (nullable)
     * @param tariffRate The tariff rate (nullable)
     * @return Canonical string representation of the row
     */
    private static String serializeRow(Long tradeId, Long productId, Long countryId, BigDecimal tariffRate) {
        StringBuilder sb = new StringBuilder();

        // Trade ID (required field)
        sb.append(tradeId);
        sb.append('|');

        // Product ID (nullable)
        if (productId != null) {
            sb.append(productId);
        } else {
            sb.append("NULL");
        }
        sb.append('|');

        // Country ID (nullable)
        if (countryId != null) {
            sb.append(countryId);
        } else {
            sb.append("NULL");
        }
        sb.append('|');

        // Tariff Rate (nullable) - normalize decimal representation
        if (tariffRate != null) {
            // Strip trailing zeros and use plain string (no scientific notation)
            sb.append(tariffRate.stripTrailingZeros().toPlainString());
        } else {
            sb.append("NULL");
        }

        return sb.toString();
    }
}
