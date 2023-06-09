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

package xcall.sample.spoketoken;

import static java.math.BigInteger.TEN;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.math.BigInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.iconloop.score.test.Account;
import com.iconloop.score.test.Score;
import com.iconloop.score.test.ServiceManager;
import com.iconloop.score.test.TestBase;

import icon.xcall.lib.messages.SpokeTokenMessages;
// import icon.xcall.lib.util.CrossChainAddress;
import xcall.score.lib.interfaces.XTokenReceiver;
import xcall.score.lib.interfaces.XTokenReceiverScoreInterface;
import xcall.score.lib.test.MockContract;
import xcall.score.lib.util.NetworkAddress;

class SpokeTokenTest extends TestBase {
    private static final String name = "MyIRC2Token";
    private static final String symbol = "MIT";
    private static final BigInteger decimals = BigInteger.valueOf(18);

    private static final BigInteger totalSupply = BigInteger.valueOf(100000).multiply(TEN.pow(decimals.intValue()));
    private static final ServiceManager sm = getServiceManager();
    private static final Account owner = sm.createAccount();
    private static final Account xcall = sm.createAccount();

    private static Score tokenScore;

    private static MockContract<XTokenReceiver> receiverContract;
    private static SpokeTokenBase tokenSpy;
    private static String ICON_NID = "01.ICON";

    @BeforeEach
    public void setup() throws Exception {
        tokenScore = sm.deploy(owner, SpokeTokenBase.class,
            ICON_NID, name, symbol, decimals, totalSupply);

        tokenSpy = (SpokeTokenBase) spy(tokenScore.getInstance());
        tokenScore.setInstance(tokenSpy);
        receiverContract = new MockContract<>(XTokenReceiverScoreInterface.class, sm, owner);

        tokenScore.invoke(owner, "setXCallManager", xcall.getAddress());
    }

    @Test
    void transfer_ICONUserToICONUser() {
        // Arrange
        Account alice = sm.createAccount();
        Account bob = sm.createAccount();
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "transfer", bob.getAddress(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        verify(tokenSpy).Transfer(alice.getAddress(), bob.getAddress(), amount, new byte[0]);
    }

    @Test
    void hubTransfer_ICONUserToICONUser() {
         // Arrange
        Account alice = sm.createAccount();
        Account bob = sm.createAccount();
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "hubTransfer", bob.getAddress().toString(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        verify(tokenSpy).Transfer(alice.getAddress(), bob.getAddress(), amount, new byte[0]);
    }

    @Test
    void transfer_ICONUserToICONContract() {
        // Arrange
        Account alice = sm.createAccount();
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "transfer", receiverContract.getAddress(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(receiverContract.account));
        verify(tokenSpy).Transfer(alice.getAddress(), receiverContract.getAddress(), amount, new byte[0]);
        verify(receiverContract.mock).tokenFallback(alice.getAddress(), amount, new byte[0]);
    }

    @Test
    void hubTransfer_ICONUserToICONContract() {
        // Arrange
        Account alice = sm.createAccount();
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "hubTransfer", receiverContract.getAddress().toString(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(receiverContract.account));
        verify(tokenSpy).Transfer(alice.getAddress(), receiverContract.getAddress(), amount, new byte[0]);
        verify(receiverContract.mock).tokenFallback(alice.getAddress(), amount, new byte[0]);    }


    @Test
    void hubTransfer_ICONUserToXCallUser() {
        // Arrange
        Account alice = sm.createAccount();
        NetworkAddress bob = new NetworkAddress("01.eth", "0x1");
        BigInteger amount = BigInteger.TWO.pow(18);
        addBalance(alice, amount);

        // Act
        tokenScore.invoke(alice, "hubTransfer", bob.toString(), amount, new byte[0]);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        verify(tokenSpy).HubTransfer(new NetworkAddress(ICON_NID, alice.getAddress()).toString(), bob.toString(), amount, new byte[0]);
    }

    @Test
    void hubTransfer_XCallUserToICONUser() {
        // Arrange
        NetworkAddress alice = new NetworkAddress("01.eth", "0x1");
        Account bob = sm.createAccount();
        BigInteger amount = BigInteger.TWO.multiply(BigInteger.TEN.pow(18));
        addBalance(alice, amount);

        // Act
        byte[] msg = SpokeTokenMessages.xHubTransfer(new NetworkAddress(ICON_NID, bob.getAddress()).toString(), amount, new byte[0]);

        tokenScore.invoke(xcall, "handleCallMessage", alice.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(bob));
        verify(tokenSpy).HubTransfer(alice.toString(), new NetworkAddress(ICON_NID, bob.getAddress()).toString(), amount, new byte[0]);
    }

    @Test
    void hubTransfer_XCallUserToICONContract() {
        // Arrange
        NetworkAddress alice = new NetworkAddress("01.eth", "0x1");
        BigInteger amount = BigInteger.TWO.pow(18);
        NetworkAddress receiverContractNetworkAddress = new NetworkAddress(ICON_NID, receiverContract.getAddress().toString());
        addBalance(alice, amount);

        // Act
        byte[] msg = SpokeTokenMessages.xHubTransfer(receiverContractNetworkAddress.toString(), amount, new byte[0]);
        tokenScore.invoke(xcall, "handleCallMessage", alice.toString(), msg);

        // Assert
        assertEquals(BigInteger.ZERO, balanceOf(alice));
        assertEquals(amount, balanceOf(receiverContract.account));
        verify(tokenSpy).HubTransfer(alice.toString(), new NetworkAddress(ICON_NID,receiverContract.getAddress()).toString(), amount, new byte[0]);
        verify(receiverContract.mock).xTokenFallback(alice.toString(), amount, new byte[0]);
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