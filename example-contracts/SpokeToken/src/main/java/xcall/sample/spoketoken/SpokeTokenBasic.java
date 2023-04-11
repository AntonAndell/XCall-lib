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

package xcall.sample.spoketoken;

import score.Address;
import score.Context;
import score.DictDB;
import score.VarDB;
import score.annotation.EventLog;
import score.annotation.External;
import score.annotation.Optional;
import xcall.score.lib.interfaces.SpokeToken;
import xcall.score.lib.interfaces.SpokeTokenXCall;
import xcall.score.lib.util.NetworkAddress;

import java.math.BigInteger;

public abstract class SpokeTokenBasic implements SpokeToken {
    private final static String NAME = "name";
    private final static String SYMBOL = "symbol";
    private final static String DECIMALS = "decimals";
    private final static String TOTAL_SUPPLY = "total_supply";
    private final static String BALANCES = "balances";
    private final static String XCALL_Manager = "xcall_manager";
    public static String NATIVE_NID;

    static final Address ZERO_ADDRESS = new Address(new byte[Address.LENGTH]);

    private final VarDB<String> name = Context.newVarDB(NAME, String.class);
    private final VarDB<String> symbol = Context.newVarDB(SYMBOL, String.class);
    private final VarDB<BigInteger> decimals = Context.newVarDB(DECIMALS, BigInteger.class);
    private final VarDB<BigInteger> totalSupply = Context.newVarDB(TOTAL_SUPPLY, BigInteger.class);
    protected final DictDB<String, BigInteger> balances = Context.newDictDB(BALANCES, BigInteger.class);
    protected final VarDB<Address> xCallManager = Context.newVarDB(XCALL_Manager, Address.class);

    public SpokeTokenBasic(String nid, String _tokenName, String _symbolName, @Optional BigInteger _decimals) {
        NATIVE_NID = nid;
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
    public String nid() {
        return NATIVE_NID;
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
        NetworkAddress address =  new NetworkAddress(NATIVE_NID, _owner);
        return balances.getOrDefault(address.toString(), BigInteger.ZERO);
    }

    @External(readonly = true)
    public BigInteger xBalanceOf(String _owner) {
        NetworkAddress address = NetworkAddress.valueOf(_owner);
        return balances.getOrDefault(address.toString(), BigInteger.ZERO);
    }

    @External
    public void setXCallManager(Address manager) {
        Context.require(Context.getCaller().equals(Context.getOwner()), "Only owner can set XCall manager");
        xCallManager.set(manager);
    }

    @External(readonly = true)
    public Address getXCallManager() {
        return xCallManager.get();
    }

    @External
    public void transfer(Address _to, BigInteger _value, @Optional byte[] _data) {
        transfer(
            new NetworkAddress(NATIVE_NID, Context.getCaller()),
            new NetworkAddress(NATIVE_NID, _to),
            _value,
            _data);
    }

    @External
    public void hubTransfer(String _to, BigInteger _value, @Optional byte[] _data) {
        transfer(
            new NetworkAddress(NATIVE_NID, Context.getCaller()),
            NetworkAddress.valueOf(_to.toString(), NATIVE_NID),
            _value,
            _data);
    }


    public void xHubTransfer(String from, String _to, BigInteger _value, byte[] _data) {
        transfer(
            NetworkAddress.valueOf(from),
            NetworkAddress.valueOf(_to.toString()),
            _value,
            _data);
    }

    @External
    public void handleCallMessage(String _from, byte[] _data) {
        Context.require(Context.getCaller().equals(xCallManager.get()));
        SpokeTokenXCall.process(this, _from, _data);
    }

    protected void transfer(NetworkAddress _from, NetworkAddress _to, BigInteger _value, byte[] _data) {
        Context.require(_value.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": _value needs to be positive");
        BigInteger fromBalance = balances.getOrDefault(_from.toString(), BigInteger.ZERO);

        Context.require(fromBalance.compareTo(_value) >= 0, this.name.get() + ": Insufficient balance");

        this.balances.set(_from.toString(), fromBalance.subtract(_value));
        this.balances.set(_to.toString(), balances.getOrDefault(_to.toString(), BigInteger.ZERO).add(_value));

        byte[] dataBytes = (_data == null) ? "None".getBytes() : _data;
        if (isNative(_to) && isNative(_from)) {
            Transfer(Address.fromString(_from.account()), Address.fromString(_to.account()), _value, dataBytes);

        } else {
            HubTransfer(_from.toString(), _to.toString(), _value, dataBytes);
        }

        if (!_to.net().equals(NATIVE_NID)) {
            return;
        }


        Address contractAddress = Address.fromString(_to.account());
        if (!contractAddress.isContract()) {
            return;
        }

        if (isNative(_from)) {
            Context.call(contractAddress, "tokenFallback", Address.fromString(_from.account()), _value, dataBytes);
        } else {
            Context.call(contractAddress, "xTokenFallback", _from.toString(), _value, dataBytes);
        }
    }

    protected void mint(NetworkAddress minter, BigInteger amount) {
        _mint(minter, amount);
        if (isNative(minter)) {
            Transfer(ZERO_ADDRESS, Address.fromString(minter.account()), amount, "mint".getBytes());
        } else {
            HubTransfer(ZERO_ADDRESS.toString(), minter.toString(), amount, null);
        }
    }

    protected void _mint(NetworkAddress minter, BigInteger amount) {
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": Amount needs to be positive");

        totalSupply.set(totalSupply().add(amount));
        balances.set(minter.toString(), balances.getOrDefault(minter.toString(), BigInteger.ZERO).add(amount));
    }

    protected void burn(NetworkAddress owner, BigInteger amount) {
        _burn(owner, amount);
        if (isNative(owner)) {
            Transfer(Address.fromString(owner.account()), ZERO_ADDRESS, amount, "mint".getBytes());
        } else {
            HubTransfer(owner.toString(), ZERO_ADDRESS.toString(), amount, null);
        }
    }

    protected void _burn(NetworkAddress owner, BigInteger amount) {
        Context.require(!ZERO_ADDRESS.equals(owner), this.name.get() + ": Owner address cannot be zero address");
        Context.require(amount.compareTo(BigInteger.ZERO) >= 0, this.name.get() + ": Amount needs to be positive");
        BigInteger balance = balances.getOrDefault(owner.toString(), BigInteger.ZERO);
        Context.require(balance.compareTo(amount) >= 0, this.name.get() + ": Insufficient Balance");

        balances.set(owner.toString(), balance.subtract(amount));
        totalSupply.set(totalSupply().subtract(amount));

    }

    protected boolean isNative(NetworkAddress address) {
        return address.net().equals(NATIVE_NID);
    }
}
