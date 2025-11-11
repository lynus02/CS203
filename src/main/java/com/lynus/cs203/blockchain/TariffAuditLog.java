package com.lynus.cs203.blockchain;

import io.reactivex.Flowable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import org.web3j.abi.EventEncoder;
import org.web3j.abi.TypeReference;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Event;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.Utf8String;
import org.web3j.abi.datatypes.generated.Uint256;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameter;
import org.web3j.protocol.core.RemoteCall;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.request.EthFilter;
import org.web3j.protocol.core.methods.response.BaseEventResponse;
import org.web3j.protocol.core.methods.response.Log;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.tuples.generated.Tuple3;
import org.web3j.tx.Contract;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.gas.ContractGasProvider;

/**
 * <p>Auto generated code.
 * <p><strong>Do not modify!</strong>
 * <p>Please use the <a href="https://docs.web3j.io/command_line.html">web3j command line tools</a>,
 * or the org.web3j.codegen.SolidityFunctionWrapperGenerator in the 
 * <a href="https://github.com/LFDT-web3j/web3j/tree/main/codegen">codegen module</a> to update.
 *
 * <p>Generated with web3j version 4.14.0.
 */
@SuppressWarnings("rawtypes")
public class TariffAuditLog extends Contract {
    public static final String BINARY = "6080604052348015600e575f5ffd5b506106508061001c5f395ff3fe608060405234801561000f575f5ffd5b506004361061003f575f3560e01c806371dc61cb14610043578063ac1be2d014610058578063e79899bd14610076575b5f5ffd5b61005661005136600461033e565b610098565b005b610060610165565b60405161006d919061041f565b60405180910390f35b610089610084366004610438565b610266565b60405161006d9392919061044f565b6040805160608101825282815242602082015233918101919091525f8054600181018255908052815160039091027f290decd9548b62a8d60345a988386fc84ba6bc95484008f6362f93160ef3e563019081906100f59082610505565b5060208201516001820155604091820151600290910180546001600160a01b0319166001600160a01b039092169190911790555133907faea7310eab8defd2229b800a4f9aa9397e8fa2e614f2721e5a34ded9221da4f99061015a90849042906105c0565b60405180910390a250565b5f546060906101ad5760405162461bcd60e51b815260206004820152601060248201526f139bc81a185cda195cc81cdd1bdc995960821b604482015260640160405180910390fd5b5f80546101bc906001906105e1565b815481106101cc576101cc610606565b905f5260205f2090600302015f0180546101e590610481565b80601f016020809104026020016040519081016040528092919081815260200182805461021190610481565b801561025c5780601f106102335761010080835404028352916020019161025c565b820191905f5260205f20905b81548152906001019060200180831161023f57829003601f168201915b5050505050905090565b5f8181548110610274575f80fd5b905f5260205f2090600302015f91509050805f01805461029390610481565b80601f01602080910402602001604051908101604052809291908181526020018280546102bf90610481565b801561030a5780601f106102e15761010080835404028352916020019161030a565b820191905f5260205f20905b8154815290600101906020018083116102ed57829003601f168201915b5050505060018301546002909301549192916001600160a01b0316905083565b634e487b7160e01b5f52604160045260245ffd5b5f6020828403121561034e575f5ffd5b813567ffffffffffffffff811115610364575f5ffd5b8201601f81018413610374575f5ffd5b803567ffffffffffffffff81111561038e5761038e61032a565b604051601f8201601f19908116603f0116810167ffffffffffffffff811182821017156103bd576103bd61032a565b6040528181528282016020018610156103d4575f5ffd5b816020840160208301375f91810160200191909152949350505050565b5f81518084528060208401602086015e5f602082860101526020601f19601f83011685010191505092915050565b602081525f61043160208301846103f1565b9392505050565b5f60208284031215610448575f5ffd5b5035919050565b606081525f61046160608301866103f1565b6020830194909452506001600160a01b0391909116604090910152919050565b600181811c9082168061049557607f821691505b6020821081036104b357634e487b7160e01b5f52602260045260245ffd5b50919050565b601f82111561050057805f5260205f20601f840160051c810160208510156104de5750805b601f840160051c820191505b818110156104fd575f81556001016104ea565b50505b505050565b815167ffffffffffffffff81111561051f5761051f61032a565b6105338161052d8454610481565b846104b9565b6020601f821160018114610565575f831561054e5750848201515b5f19600385901b1c1916600184901b1784556104fd565b5f84815260208120601f198516915b828110156105945787850151825560209485019460019092019101610574565b50848210156105b157868401515f19600387901b60f8161c191681555b50505050600190811b01905550565b604081525f6105d260408301856103f1565b90508260208301529392505050565b8181038181111561060057634e487b7160e01b5f52601160045260245ffd5b92915050565b634e487b7160e01b5f52603260045260245ffdfea26469706673582212206c6066deea13c607b4e159c22aec45bda90f1dd85e81ff4c3224711a108df3f364736f6c634300081d0033";

    private static String librariesLinkedBinary;

    public static final String FUNC_GETLATESTHASH = "getLatestHash";

    public static final String FUNC_LOGS = "logs";

    public static final String FUNC_STOREHASH = "storeHash";

    public static final Event HASHSTORED_EVENT = new Event("HashStored", 
            Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Address>(true) {}, new TypeReference<Uint256>() {}));
    ;

    @Deprecated
    protected TariffAuditLog(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    protected TariffAuditLog(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, credentials, contractGasProvider);
    }

    @Deprecated
    protected TariffAuditLog(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        super(BINARY, contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    protected TariffAuditLog(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        super(BINARY, contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static List<HashStoredEventResponse> getHashStoredEvents(
            TransactionReceipt transactionReceipt) {
        List<Contract.EventValuesWithLog> valueList = staticExtractEventParametersWithLog(HASHSTORED_EVENT, transactionReceipt);
        ArrayList<HashStoredEventResponse> responses = new ArrayList<HashStoredEventResponse>(valueList.size());
        for (Contract.EventValuesWithLog eventValues : valueList) {
            HashStoredEventResponse typedResponse = new HashStoredEventResponse();
            typedResponse.log = eventValues.getLog();
            typedResponse.uploader = (String) eventValues.getIndexedValues().get(0).getValue();
            typedResponse.dbHash = (String) eventValues.getNonIndexedValues().get(0).getValue();
            typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
            responses.add(typedResponse);
        }
        return responses;
    }

    public static HashStoredEventResponse getHashStoredEventFromLog(Log log) {
        Contract.EventValuesWithLog eventValues = staticExtractEventParametersWithLog(HASHSTORED_EVENT, log);
        HashStoredEventResponse typedResponse = new HashStoredEventResponse();
        typedResponse.log = log;
        typedResponse.uploader = (String) eventValues.getIndexedValues().get(0).getValue();
        typedResponse.dbHash = (String) eventValues.getNonIndexedValues().get(0).getValue();
        typedResponse.timestamp = (BigInteger) eventValues.getNonIndexedValues().get(1).getValue();
        return typedResponse;
    }

    public Flowable<HashStoredEventResponse> hashStoredEventFlowable(EthFilter filter) {
        return web3j.ethLogFlowable(filter).map(log -> getHashStoredEventFromLog(log));
    }

    public Flowable<HashStoredEventResponse> hashStoredEventFlowable(
            DefaultBlockParameter startBlock, DefaultBlockParameter endBlock) {
        EthFilter filter = new EthFilter(startBlock, endBlock, getContractAddress());
        filter.addSingleTopic(EventEncoder.encode(HASHSTORED_EVENT));
        return hashStoredEventFlowable(filter);
    }

    public RemoteFunctionCall<String> getLatestHash() {
        final Function function = new Function(FUNC_GETLATESTHASH, 
                Arrays.<Type>asList(), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}));
        return executeRemoteCallSingleValueReturn(function, String.class);
    }

    public RemoteFunctionCall<Tuple3<String, BigInteger, String>> logs(BigInteger param0) {
        final Function function = new Function(FUNC_LOGS, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.generated.Uint256(param0)), 
                Arrays.<TypeReference<?>>asList(new TypeReference<Utf8String>() {}, new TypeReference<Uint256>() {}, new TypeReference<Address>() {}));
        return new RemoteFunctionCall<Tuple3<String, BigInteger, String>>(function,
                new Callable<Tuple3<String, BigInteger, String>>() {
                    @Override
                    public Tuple3<String, BigInteger, String> call() throws Exception {
                        List<Type> results = executeCallMultipleValueReturn(function);
                        return new Tuple3<String, BigInteger, String>(
                                (String) results.get(0).getValue(), 
                                (BigInteger) results.get(1).getValue(), 
                                (String) results.get(2).getValue());
                    }
                });
    }

    public RemoteFunctionCall<TransactionReceipt> storeHash(String _dbHash) {
        final Function function = new Function(
                FUNC_STOREHASH, 
                Arrays.<Type>asList(new org.web3j.abi.datatypes.Utf8String(_dbHash)), 
                Collections.<TypeReference<?>>emptyList());
        return executeRemoteCallTransaction(function);
    }

    @Deprecated
    public static TariffAuditLog load(String contractAddress, Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return new TariffAuditLog(contractAddress, web3j, credentials, gasPrice, gasLimit);
    }

    @Deprecated
    public static TariffAuditLog load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return new TariffAuditLog(contractAddress, web3j, transactionManager, gasPrice, gasLimit);
    }

    public static TariffAuditLog load(String contractAddress, Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return new TariffAuditLog(contractAddress, web3j, credentials, contractGasProvider);
    }

    public static TariffAuditLog load(String contractAddress, Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return new TariffAuditLog(contractAddress, web3j, transactionManager, contractGasProvider);
    }

    public static RemoteCall<TariffAuditLog> deploy(Web3j web3j, Credentials credentials,
            ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TariffAuditLog.class, web3j, credentials, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<TariffAuditLog> deploy(Web3j web3j, Credentials credentials,
            BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TariffAuditLog.class, web3j, credentials, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static RemoteCall<TariffAuditLog> deploy(Web3j web3j,
            TransactionManager transactionManager, ContractGasProvider contractGasProvider) {
        return deployRemoteCall(TariffAuditLog.class, web3j, transactionManager, contractGasProvider, getDeploymentBinary(), "");
    }

    @Deprecated
    public static RemoteCall<TariffAuditLog> deploy(Web3j web3j,
            TransactionManager transactionManager, BigInteger gasPrice, BigInteger gasLimit) {
        return deployRemoteCall(TariffAuditLog.class, web3j, transactionManager, gasPrice, gasLimit, getDeploymentBinary(), "");
    }

    public static void linkLibraries(List<Contract.LinkReference> references) {
        librariesLinkedBinary = linkBinaryWithReferences(BINARY, references);
    }

    private static String getDeploymentBinary() {
        if (librariesLinkedBinary != null) {
            return librariesLinkedBinary;
        } else {
            return BINARY;
        }
    }

    public static class HashStoredEventResponse extends BaseEventResponse {
        public String uploader;

        public String dbHash;

        public BigInteger timestamp;
    }
}
