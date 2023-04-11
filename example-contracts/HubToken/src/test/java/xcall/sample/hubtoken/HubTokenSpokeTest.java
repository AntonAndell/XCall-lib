package xcall.sample.hubtoken;


import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.web3j.EVMTest;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.tx.TransactionManager;
import org.web3j.tx.Transfer;
import org.web3j.tx.RawTransactionManager;

import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.utils.Convert.Unit;

import com.iconloop.score.test.TestBase;

import icon.xcall.lib.messages.HubTokenMessages;
import xcall.score.lib.util.NetworkAddress;
import xcall.score.lib.util.ProtocolPrefixNetworkAddress;

@EVMTest()
public class HubTokenSpokeTest extends TestBase {
    private static String ethNid = "1.ETH";

    private Credentials godWallet = Credentials.create("0x8f2a55949038a9610f50fb23b5883af3b4ecb3c3bb792cbcefbd1542c692be63");
    private ProtocolPrefixNetworkAddress hubProtocolAddress = new ProtocolPrefixNetworkAddress("ICON", "account");
    private NetworkAddress hubAddress = new NetworkAddress(hubProtocolAddress);

    private Web3j web3j;
    private TransactionManager transactionManager;
    private ContractGasProvider gasProvider;

    private XCallMock xCallMock;
    private HubTokenSpoke token;

    @BeforeEach
    public void setup(Web3j web3j, TransactionManager transactionManager, ContractGasProvider gasProvider) throws Exception{
        this.web3j = web3j;
        this.transactionManager = transactionManager;
        this.gasProvider = gasProvider;

        this.xCallMock = XCallMock.deploy(web3j, godWallet, gasProvider, ethNid).send();
        this.token = HubTokenSpoke.deploy(web3j, godWallet, gasProvider, xCallMock.getContractAddress(), hubAddress.toString()).send();
    }

    @Test
    public void crossTransfer() throws Exception {
        Credentials bob = createWallet("bob");
        NetworkAddress alice = new NetworkAddress("ICON", "alice");

        BigInteger amount = BigInteger.TEN.pow(20);
        addBalance(bob, amount);

        newTokenUser(bob).crossTransfer(alice.toString(), amount, new byte[0], BigInteger.ZERO).send();

        assertEquals(BigInteger.ZERO, token.balanceOf(bob.getAddress()).send());
        assertEquals(BigInteger.ZERO, token.totalSupply().send());
    }

    @Test
    public void crossTransferRollback() throws Exception {
        Credentials bob = createWallet("bob");

        BigInteger amount = BigInteger.TEN.pow(20);
        NetworkAddress address = new NetworkAddress(ethNid, bob.getAddress());
        byte[] data = HubTokenMessages.xCrossTransferRevert(address.toString(), amount);
        xCallMock.rollback(token.getContractAddress(), data).send();

        assertEquals(amount, token.balanceOf(bob.getAddress()).send());
        assertEquals(amount, token.totalSupply().send());
    }

    private Credentials createWallet(String id) throws Exception {
        Credentials credentials = Credentials.create("123");
        Transfer.sendFunds(web3j, godWallet, credentials.getAddress(), BigDecimal.ONE, Unit.ETHER).send();

        return credentials;
    }

    private void addBalance(Credentials credentials, BigInteger amount) throws Exception {
        NetworkAddress address = new NetworkAddress(ethNid, credentials.getAddress());
        byte[] data = HubTokenMessages.xCrossTransfer(address.toString(), address.toString(), amount, new byte[0]);
        xCallMock.execute(hubProtocolAddress.toString(), token.getContractAddress(), data).send();
    }

    private HubTokenSpoke newTokenUser(Credentials credentials) {
        return HubTokenSpoke.load(token.getContractAddress(), web3j, credentials, gasProvider);
    }

}