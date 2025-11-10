package com.lynus.cs203.blockchain;

import java.math.BigInteger;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.Collections;
import java.sql.*;

import org.web3j.protocol.Web3j;
import org.web3j.protocol.http.HttpService;
import org.web3j.crypto.Credentials;
import org.web3j.tx.RawTransactionManager;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.StaticGasProvider;
import org.web3j.utils.Convert;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.utils.Numeric;
import java.nio.charset.StandardCharsets;

// This file runs once to deploy the compiled solidity contract (TariffAuditLog) and record the hash of the original (non-malicious) db. Meant to be run once only.
public class DeployTariffAuditLog {

    private static final String LIVE_DB_URL = System.getenv("LIVE_DB_URL");
    private static final String LIVE_DB_USER = System.getenv("LIVE_DB_USERNAME");
    private static final String LIVE_DB_PASS = System.getenv("LIVE_DB_PASSWORD");

    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection(LIVE_DB_URL, LIVE_DB_USER, LIVE_DB_PASS);

        // 1. Hash the tariff table
        String tariffHash = hashTariffTable(conn);
        System.out.println("Tariff table hash: " + tariffHash);

        // --- Environment Setup ---
        String privateKey = System.getenv("BLOCKCHAIN_PRIVATE_KEY");
        String chainIdStr = System.getenv("BLOCKCHAIN_CHAIN_ID");
        String rpc = System.getenv("BLOCKCHAIN_RPC_URL");
        String contractAddressEnv = System.getenv("BLOCKCHAIN_CONTRACT_ADDRESS");

        if (privateKey == null || privateKey.isEmpty()) {
            throw new IllegalStateException("`BLOCKCHAIN_PRIVATE_KEY` must be set");
        }
        if (chainIdStr == null || chainIdStr.isEmpty()) {
            throw new IllegalStateException("`BLOCKCHAIN_CHAIN_ID` must be set");
        }
        if (rpc == null || rpc.isEmpty()) {
            throw new IllegalStateException("`BLOCKCHAIN_RPC_URL` must be set");
        }

        // --- Gas Settings ---
        String gasPriceGweiStr = System.getenv().getOrDefault("BLOCKCHAIN_GAS_PRICE_GWEI", "20");
        String gasLimitStr = System.getenv().getOrDefault("BLOCKCHAIN_GAS_LIMIT", "3000000");

        BigDecimal gasPriceGwei = new BigDecimal(gasPriceGweiStr);
        BigInteger gasPriceWei = Convert.toWei(gasPriceGwei, Convert.Unit.GWEI).toBigInteger();
        BigInteger gasLimit = new BigInteger(gasLimitStr);

        BigInteger feeWei = gasPriceWei.multiply(gasLimit);
        BigDecimal feeEth = Convert.fromWei(new BigDecimal(feeWei), Convert.Unit.ETHER);

        System.out.println("🔗 RPC: " + rpc);
        System.out.println("⛽ Gas price: " + gasPriceGwei + " GWei");
        System.out.println("⛽ Gas limit: " + gasLimit);
        System.out.println("💰 Estimated max fee: " + feeEth.toPlainString() + " ETH");

        long chainId = Long.parseLong(chainIdStr);

        // --- Connect to blockchain ---
        Web3j web3j = Web3j.build(new HttpService(rpc));
        Credentials creds = Credentials.create(privateKey);
        String account = creds.getAddress();
        TransactionManager txManager = new RawTransactionManager(web3j, creds, chainId);

        // --- Check balance ---
        BigInteger balanceWei = web3j.ethGetBalance(account, DefaultBlockParameterName.LATEST)
                .send().getBalance();
        BigDecimal balanceEth = Convert.fromWei(new BigDecimal(balanceWei), Convert.Unit.ETHER);

        System.out.println("👤 Account: " + account);
        System.out.println("💵 Balance: " + balanceEth.toPlainString() + " ETH");
        if (balanceWei.compareTo(feeWei) < 0) {
            System.err.println("❌ Insufficient funds: " + balanceEth.toPlainString() + " ETH < required " + feeEth);
            web3j.shutdown();
            System.exit(1);
        }

        StaticGasProvider gasProvider = new StaticGasProvider(gasPriceWei, gasLimit);

        // --- Deploy or Load Contract ---
        TariffAuditLog contract;
        if (contractAddressEnv != null && contractAddressEnv.matches("^0x[0-9a-fA-F]{40}$")) {
            contract = TariffAuditLog.load(contractAddressEnv, web3j, txManager, gasProvider);
            System.out.println("📄 Loaded existing contract at: " + contract.getContractAddress());
        } else {
            System.out.println("🚀 No existing contract detected — deploying new TariffAuditLog...");
            contract = TariffAuditLog.deploy(web3j, txManager, gasProvider).send();
            System.out.println("✅ Deployed new contract at: " + contract.getContractAddress());
        }

        // --- Send hash to blockchain ---
        var receipt = contract.storeHash(tariffHash).send();
        System.out.println("Hash recorded on blockchain, tx hash: " + receipt.getTransactionHash());
        System.out.println("🧾 Tx complete! Hash: " + receipt.getTransactionHash());
        System.out.println("📜 Contract Address: " + contract.getContractAddress());

        conn.close();
        web3j.shutdown();
    }

    // --- Utility methods ---
    private static String hashTariffTable(Connection conn) throws Exception {
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

            // MySQL streaming mode to handle large datasets efficiently
            ps.setFetchSize(Integer.MIN_VALUE);

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