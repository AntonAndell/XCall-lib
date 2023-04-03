/*
 * Copyright (c) 2022-2023 Balanced.network.
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

package icon.xcall.spoketoken;

import score.*;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import icon.xcall.lib.util.CrossChainAddress;

public class SpokeTokenImpl implements SpokeToken {
    private final static String NAME = "name";
    private final static String SYMBOL = "symbol";
    private final static String DECIMALS = "decimals";
    private final static String TOTAL_SUPPLY = "total_supply";
    private final static String BALANCES = "balances";

    static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    private final VarDB<String> name = Context.newVarDB(NAME, String.class);
    private final VarDB<String> symbol = Context.newVarDB(SYMBOL, String.class);
    private final VarDB<BigInteger> decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    private final VarDB<BigInteger> totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    protected final DictDB<CrossChainAddress, BigInteger> balances = Context.newDictDB(BALANCES, BigInteger.class);

    SpokeTokenImpl(String _tokenName, String _symbolName, @Optional BigInteger _decimals) {
        if (this.name.get() == null) {
            _decimals = _decimals == null ? BigInteger.valueOf(18L) : _decimals;
            Context.require(_decimals.compareTo(BigInteger.ZERO) >= 0, "Decimals cannot be less than zero");

            this.name.set(ensureNotEmpty(_tokenName));
            this.symbol.set(ensureNotEmpty(_symbolName));
            this.decimals.set(_decimals);
        }
    }

    @EventLog(indexed = 3)
    public void Transfer(Address _from, Address _to, BigInteger _value, byte[] _data) {
    }

    @EventLog(indexed = 3)
    public void HubTransfer(String _from, String _to, BigInteger _value, byte[] _data) {
    }

    private String ensureNotEmpty(String str) {
        Context.require(str != null && !str.trim().isEmpty(), "str is null or empty");
        assert str != null;
        return str.trim();
    }

    @External(readonly = true)
    public String name() {
        return name.get();
    }

    @External(readonly = true)
    public String symbol() {
        return symbol.get();
    }

    @External(readonly = true)
    public BigInteger decimals() {
        return decimals.get();
    }

    @External(readonly = true)
    public BigInteger totalSupply() {
        return totalSupply.getOrDefault(BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger balanceOf(Address _owner) {
        CrossChainAddress address =  new CrossChainAddress(_owner.toString());
        return balances.getOrDefault(address, BigInteger.ZERO);
    }

        
    public BigInteger xBalanceOf(String _owner) {
        CrossChainAddress address = CrossChainAddress.parse(_owner);
        return balances.getOrDefault(address, BigInteger.ZERO);   
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        transfer(
            new CrossChainAddress(Context.getCaller().toString()), 
            new CrossChainAddress(_to.toString()), 
            _value,
            _data);
    }

    @External
    public void hubTransfer(String _to, BigInteger _value, @Optional byte[] _data) {
        transfer(
            new CrossChainAddress(Context.getCaller().toString()), 
            CrossChainAddress.parse(_to.toString()), 
            _value,
            _data);
    }
    
    
    public void hubTransfer(String from, String _to, BigInteger _value, byte[] _data) {
        transfer(
            CrossChainAddress.parse(from), 
            CrossChainAddress.parse(_to.toString()), 
            _value,
            _data);
    }

    @External
    void handleCallMessage(String _from, byte[] _data) {
        // Verify caller is XCall contract or other allowed contract.
        SpokeTokenXCall.process(this, _from, _data);
    }

    protected void transfer(CrossChainAddress _from, CrossChainAddress _to, BigInteger _value, byte[] _data) {
        Context.require(_value.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": _value needs to be positive");
        BigInteger fromBalance = balances.getOrDefault(_from, BigInteger.ZERO);
        BigInteger toBalance = balances.getOrDefault(_to, BigInteger.ZERO);
        
        Context.require(fromBalance.compareTo(_value) >= 0, this.name.get() + ": Insufficient balance");

        this.balances.set(_from, fromBalance.subtract(_value));
        this.balances.set(_to, toBalance.add(_value));

        byte[] dataBytes = (_data == null) ? "None".getBytes() : _data;
        if (_to.isNative() && _from.isNative()) {
            Transfer(Address.fromString(_from.account()), Address.fromString(_to.account()), _value, dataBytes);

        } else {
            HubTransfer(_from.toString(), _to.toString(), _value, dataBytes);
        }

        if (!_to.isNative()) {
            return;
        }


        Address contractAddress = Address.fromString(_to.account());
        if (!contractAddress.isContract()) {
            return;
        }

        if (_from.isNative()) {
            Context.call(contractAddress, "tokenFallback", Address.fromString(_from.account()), _value, dataBytes);
        } else {

            Context.call(contractAddress, "xTokenFallback", _from.toString(), _value, dataBytes);
        }
    }

    protected void mint(CrossChainAddress minter, BigInteger amount) {
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": Amount needs to be positive");

        totalSupply.set(totalSupply().add(amount));
        balances.set(minter, balances.getOrDefault(minter, BigInteger.ZERO).add(amount));
        if (minter.isNative()) {
            Transfer(ZERO_ADDRESS, Address.fromString(minter.account()), amount, "mint".getBytes());
        } else {
            HubTransfer(ZERO_ADDRESS.toString(), minter.toString(), amount, null);
        }
    }

    protected void burn(CrossChainAddress owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner), this.name.get() + ": Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": Amount needs to be positive");
        BigInteger balance = balances.getOrDefault(owner, BigInteger.ZERO);
        Context.require(balance.compareTo(amount) >= 0, this.name.get() + ": Insufficient Balance");

        balances.set(owner, balance.subtract(amount));
        totalSupply.set(totalSupply().subtract(amount));
        if (owner.isNative()) {
            Transfer(Address.fromString(owner.account()), ZERO_ADDRESS, amount, "mint".getBytes());
        } else {
            HubTransfer(owner.toString(), ZERO_ADDRESS.toString(), amount, null);
        }
    }


    
}
