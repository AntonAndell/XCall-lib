/*
 * Copyright 2020 ICONLOOP Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package xcall.sample.hubtoken;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.AdditionalMatchers;
import org.mockito.Mockito;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import icon.xcall.lib.messages.HubTokenMessages;
import xcall.score.lib.interfaces.XCall;
import xcall.score.lib.interfaces.XCallScoreInterface;
import xcall.score.lib.interfaces.XTokenReceiver;
import xcall.score.lib.interfaces.XTokenReceiverScoreInterface;
import xcall.score.lib.test.MockContract;
import xcall.score.lib.util.NetworkAddress;
import xcall.score.lib.util.ProtocolPrefixNetworkAddress;

class HubTokenTest extends TestBase {
    private static final String name = "MyIRC2Token";
    private static final String symbol = "MIT";
    private static final BigInteger decimals = BigInteger.valueOf(18);

    private static final BigInteger totalSupply = BigInteger.valueOf(100000).multiply(TEN.pow(decimals.intValue()));
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();

    private static Score tokenScore;

    private static MockContract<XTokenReceiver> receiverContract;
    private static MockContract<XCall> xCall;
    private static HubTokenBase tokenSpy;
    private static String ethNid = "1.ETH";
    private static String bscNid = "1.BSC";
    private static String ICON_NID = "1.ICON";
    private static NetworkAddress ethereumSpokeAddress = new NetworkAddress(ethNid, "0x1");
    private static NetworkAddress bscSpokeAddress = new NetworkAddress(bscNid, "0x2");


    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, HubTokenBase.class,
            ICON_NID, name, symbol, decimals, totalSupply);

        tokenSpy = (HubTokenBase) spy(tokenScore.getInstance());
        tokenScore.setInstance(tokenSpy);
        receiverContract = new MockContract<>(XTokenReceiverScoreInterface.class, sm, owner);
        xCall = new MockContract<>(XCallScoreInterface.class, sm, owner);

        tokenScore.invoke(owner, "addChain", ethereumSpokeAddress.toString());
        tokenScore.invoke(owner, "addChain", bscSpokeAddress.toString());

        tokenScore.invoke(owner, "setXCallManager", xCall.getAddress());
    }

    @Test
    void crossTransfer_ICONUserToICONUser() {
        // Arrange
        Account alice = sm.createAccount();
        Account bob = sm.createAccount();
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "crossTransfer", new NetworkAddress(ICON_NID, bob.getAddress()).toString(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        verify(tokenSpy).Transfer(alice.getAddress(), bob.getAddress(), amount, new byte[0]);
    }


   @Test
   void crossTransfer_ICONUserToSpoke() {
        // Arrange
        Account alice = sm.createAccount();
        NetworkAddress aliceNetworkAddress = new NetworkAddress(ICON_NID, alice.getAddress());
        NetworkAddress bob = new NetworkAddress(ethNid, "0x32");
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        byte[] expectedCallData = HubTokenMessages.xCrossTransfer(aliceNetworkAddress.toString(), bob.toString(), amount, new byte[0]);
        byte[] expectedRollbackData = HubTokenMessages.xCrossTransferRevert(bob.toString(), amount);

        // Act
        tokenScore.invoke(alice, "crossTransfer", bob.toString(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(BigInteger.ZERO, balanceOf(bob));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(amount, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        verify(tokenSpy).XTransfer(BigInteger.ZERO, aliceNetworkAddress.toString(), bob.toString(), amount, new byte[0]);
        verify(xCall.mock).sendCallMessage(Mockito.eq(new ProtocolPrefixNetworkAddress(ethereumSpokeAddress.net(), ethereumSpokeAddress.account()).toString()), AdditionalMatchers.aryEq(expectedCallData), AdditionalMatchers.aryEq(expectedRollbackData));
   }

   @Test
   void crossTransfer_spokeToICONUser() {
        // Arrange
        NetworkAddress alice = new NetworkAddress(ethNid, "0x32");
        Account bob = sm.createAccount();
        NetworkAddress bobNetworkAddress = new NetworkAddress(ICON_NID, bob.getAddress());
        BigInteger amount = BigInteger.TWO.pow(18);
        tokenScore.invoke(owner, "crossTransfer", alice.toString(), amount, new byte[0]);

        // Act
        byte[] msg = HubTokenMessages.xCrossTransfer(alice.toString(), bobNetworkAddress.toString(), amount, new byte[0]);
        tokenScore.invoke(xCall.account, "handleCallMessage", ethereumSpokeAddress.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", bscSpokeAddress.toString()));
        verify(tokenSpy).XTransfer(BigInteger.ZERO, alice.toString(), bobNetworkAddress.toString(), amount, new byte[0]);
   }

   @Test
   void crossTransfer_spokeToICONContract() {
        // Arrange
        NetworkAddress alice = new NetworkAddress(ethNid, "0x32");
        NetworkAddress receiverContractNetworkAddress = new NetworkAddress(ICON_NID, receiverContract.getAddress());
        BigInteger amount = BigInteger.TWO.pow(18);
        byte[] data = "test".getBytes();
        tokenScore.invoke(owner, "crossTransfer", alice.toString(), amount, data);

        // Act
        byte[] msg = HubTokenMessages.xCrossTransfer(alice.toString(), receiverContractNetworkAddress.toString(), amount, data);
        tokenScore.invoke(xCall.account, "handleCallMessage", ethereumSpokeAddress.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(receiverContract.account));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", bscSpokeAddress.toString()));
        verify(tokenSpy).XTransfer(Mockito.eq(BigInteger.ZERO), Mockito.eq(alice.toString()), Mockito.eq(receiverContractNetworkAddress.toString()), Mockito.eq(amount), AdditionalMatchers.aryEq(data));
        verify(receiverContract.mock).xTokenFallback(Mockito.eq(alice.toString()), Mockito.eq(amount), AdditionalMatchers.aryEq(data));
   }

   @Test
   void crossTransfer_ICONUserToSpoke_rollback() {
        // Arrange
        Account alice = sm.createAccount();
        NetworkAddress aliceNetworkAddress = new NetworkAddress(ICON_NID, alice.getAddress());
        NetworkAddress bob = new NetworkAddress(ethNid, "0x32");
        ProtocolPrefixNetworkAddress xCallNetworkAddress = new ProtocolPrefixNetworkAddress(ICON_NID,  xCall.getAddress().toString());
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);
        byte[] expectedCallData = HubTokenMessages.xCrossTransfer(aliceNetworkAddress.toString(), bob.toString(), amount, new byte[0]);
        byte[] expectedRollbackData = HubTokenMessages.xCrossTransferRevert(bob.toString(), amount);
        tokenScore.invoke(alice, "crossTransfer", bob.toString(), amount, new byte[0]);
        verify(xCall.mock).sendCallMessage(Mockito.eq(new ProtocolPrefixNetworkAddress(ethereumSpokeAddress.net(), ethereumSpokeAddress.account()).toString()), AdditionalMatchers.aryEq(expectedCallData), AdditionalMatchers.aryEq(expectedRollbackData));

        // Act
        tokenScore.invoke(xCall.account, "handleCallMessage", xCallNetworkAddress.toString(), expectedRollbackData);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", bscSpokeAddress.toString()));
   }

   @Test
   void crossTransfer_SpokeToSpoke() {
        // Arrange
        NetworkAddress alice = new NetworkAddress(bscNid, "0x35");
        NetworkAddress bob = new NetworkAddress(ethNid, "0x32");
        BigInteger amount = BigInteger.TWO.pow(18);
        tokenScore.invoke(owner, "crossTransfer", alice.toString(), amount, new byte[0]);

        byte[] expectedCallData = HubTokenMessages.xCrossTransfer(bob.toString(), bob.toString(), amount, new byte[0]);
        byte[] expectedRollbackData = HubTokenMessages.xCrossTransferRevert(bob.toString(), amount);

        // Act
        byte[] msg = HubTokenMessages.xCrossTransfer(alice.toString(), bob.toString(), amount, new byte[0]);
        tokenScore.invoke(xCall.account, "handleCallMessage", bscSpokeAddress.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(BigInteger.ZERO, balanceOf(bob));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(amount, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        assertEquals(BigInteger.ZERO, tokenScore.call("xSupply", bscSpokeAddress.toString()));
        verify(tokenSpy).XTransfer(BigInteger.ZERO, bob.toString(), bob.toString(), amount, new byte[0]);
        verify(xCall.mock).sendCallMessage(Mockito.eq(new ProtocolPrefixNetworkAddress(ethereumSpokeAddress.net(), ethereumSpokeAddress.account()).toString()), AdditionalMatchers.aryEq(expectedCallData), AdditionalMatchers.aryEq(expectedRollbackData));
   }

   @Test
   void withdraw() {
        // Arrange
        NetworkAddress bob = new NetworkAddress(ethNid, "0x32");
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(bob, amount);

        byte[] expectedCallData = HubTokenMessages.xCrossTransfer(bob.toString(), bob.toString(), amount, new byte[0]);
        byte[] expectedRollbackData = HubTokenMessages.xCrossTransferRevert(bob.toString(), amount);

        // Act
        byte[] msg = HubTokenMessages.xWithdraw(amount);
        tokenScore.invoke(xCall.account, "handleCallMessage", bob.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(bob));
        assertEquals(totalSupply, tokenScore.call("xTotalSupply"));
        assertEquals(amount, tokenScore.call("xSupply", ethereumSpokeAddress.toString()));
        verify(tokenSpy).XTransfer(BigInteger.ZERO, bob.toString(), bob.toString(), amount, new byte[0]);
        verify(xCall.mock).sendCallMessage(Mockito.eq(new ProtocolPrefixNetworkAddress(ethereumSpokeAddress.net(), ethereumSpokeAddress.account()).toString()), AdditionalMatchers.aryEq(expectedCallData), AdditionalMatchers.aryEq(expectedRollbackData));
   }

    void addBalance(Account account, BigInteger amount) {
        tokenScore.invoke(owner, "transfer", account.getAddress(), amount, new byte[0]);
    }

    void addBalance(NetworkAddress account, BigInteger amount) {
        tokenScore.invoke(owner, "hubTransfer", account.toString(), amount, new byte[0]);
    }

    BigInteger balanceOf(Account account) {
        return (BigInteger)tokenScore.call("balanceOf", account.getAddress());
    }

    BigInteger balanceOf(NetworkAddress account) {
        return (BigInteger)tokenScore.call("xBalanceOf", account.toString());
    }
}