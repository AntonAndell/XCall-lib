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

package xcall.sample.hubtoken;

import score.*;
import score.annotation.External;
import xcall.score.lib.util.NetworkAddress;

import java.math.BigInteger;

public class HubTokenBase extends HubTokenBasic {
    public HubTokenBase(String _nid, String _tokenName, String _symbolName, BigInteger _decimals, BigInteger _initialSupply) {
        super(_nid, _tokenName, _symbolName, _decimals);

        // mint the initial token supply here
        Context.require(_initialSupply.compareTo(BigInteger.ZERO) >= 0);
        mint(new NetworkAddress(_nid, Context.getCaller()), _initialSupply);
    }

    @External
    public void addChain(String _networkAddress) {
        Context.require(Context.getOwner().equals(Context.getCaller()), "Only owner can add new chains");
        NetworkAddress networkAddress = NetworkAddress.parse(_networkAddress);
        connectedChains.add(networkAddress);
        spokeContracts.set(networkAddress.net(), networkAddress);
    }

}
