/*
 * Copyright 2021 ICON Foundation
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
// TOOD use BTP version
package xcall.score.lib.util;


import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;

public class ProtocolPrefixNetworkAddress {
    public static final String DEFAULT_PROTOCOL_BTP="btp";
    public static final String DELIM_PROTOCOLS="|";
    private static final String DELIM_PROTOCOL="://";
    private static final String DELIM_NET="/";
    String protocols;
    String net;
    String account;

    public ProtocolPrefixNetworkAddress(String net, String account) {
        this(DEFAULT_PROTOCOL_BTP, net, account);
    }

    public ProtocolPrefixNetworkAddress(String protocols, String net, String account) {
        this.protocols = protocols;
        this.net = net;
        this.account = account;
    }

    public ProtocolPrefixNetworkAddress(String[] protocols, String net, String account) {
        this.protocols = protocolsString(protocols);
        this.net = net;
        this.account = account;
    }

    public String protocols() {
        return protocols;
    }

    public String net() {
        return net;
    }

    public String account() {
        return account;
    }

    @Override
    public String toString() {
        return protocols + DELIM_PROTOCOL + net + DELIM_NET + account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ProtocolPrefixNetworkAddress that = (ProtocolPrefixNetworkAddress) o;
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean isValid() {
        return (!(protocols == null || protocols.isEmpty())) &&
                (!(net == null || net.isEmpty())) &&
                (!(account == null || account.isEmpty()));
    }

    private static String protocolsString(String[] protocols) {
        String result = "";
        int i;
        for (i = 0; i < protocols.length-1; i++) {
            result = result + protocols[i] + "|";
        }
        result = result + protocols[++i];
        return result;
    }

    public static ProtocolPrefixNetworkAddress parse(String str) {
        if (str == null) {
            return null;
        }
        String protocol = "";
        String net = "";
        String contract = "";
        int protocolIdx = str.indexOf(DELIM_PROTOCOL);
        if(protocolIdx >= 0) {
            protocol = str.substring(0, protocolIdx);
            str = str.substring(protocolIdx + DELIM_PROTOCOL.length());
        }
        int netIdx = str.indexOf(DELIM_NET);
        if (netIdx >= 0) {
            net = str.substring(0, netIdx);
            contract = str.substring(netIdx+DELIM_NET.length());
        } else {
            contract = str;
        }
        return new ProtocolPrefixNetworkAddress(protocol, net, contract);
    }

    public static ProtocolPrefixNetworkAddress valueOf(String str) {
        ProtocolPrefixNetworkAddress btpAddress = parse(str);
        if (btpAddress == null || !btpAddress.isValid()) {
            Context.revert();
        }
        return btpAddress;
    }

    public static void writeObject(ObjectWriter writer, ProtocolPrefixNetworkAddress obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.write(this.toString());
    }

    public static ProtocolPrefixNetworkAddress readObject(ObjectReader reader) {
        return ProtocolPrefixNetworkAddress.parse(reader.readString());
    }

    public static ProtocolPrefixNetworkAddress fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return ProtocolPrefixNetworkAddress.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        ProtocolPrefixNetworkAddress.writeObject(writer, this);
        return writer.toByteArray();
    }

}