package icon.xcall.lib.util;

import java.util.Arrays;
import java.util.List;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
import scorex.util.ArrayList;
public class XCallAddress extends CrossChainAddress {
    public static final List<String> PROTOCOL_BTP= List.of("btp");
    private static final String DELIM_PROTOCOL="://";
    List<String> protocols;

    public XCallAddress(List<String> protocols, CrossChainAddress address) {
        this(protocols, address.net(), address.account());
    }

    public XCallAddress(CrossChainAddress address) {
        this(PROTOCOL_BTP, address.net(), address.account());
    }

    public XCallAddress(String net, String account) {
        this(PROTOCOL_BTP, net, account);
    }

    public XCallAddress(List<String> protocols, String net, String account) {
        super(net, account);
        this.protocols = protocols;
    }

    public List<String> protocols() {
        return protocols;
    }

    @Override
    public String toString() {
        return protocols + DELIM_PROTOCOL + net + DELIM_NET + account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        XCallAddress that = (XCallAddress) o;
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }


    @Override
    public boolean isValid() {
        return (!(protocols == null || protocols.size() == 0)) && super.isValid();
    }

    public static XCallAddress parse(String str) {
        if (str == null) {
            return null;
        }
        List<String> protocolsList = new ArrayList<>();
        int protocolIdx = str.indexOf(DELIM_PROTOCOL);
        if(protocolIdx >= 0) {
            Context.require(str.startsWith("["));
            String protocols = str.substring(1, protocolIdx);
            int nextDelimIndex;
            while (protocols.length() > 0) {
                nextDelimIndex = protocols.indexOf(",");
                if (nextDelimIndex == -1) {
                    protocolsList.add(protocols.substring(0, protocols.length()-1));
                    Context.require(protocols.endsWith("]"));
                    break;
                }
                protocolsList.add(protocols.substring(0, nextDelimIndex));
                protocols = protocols.substring(nextDelimIndex+1);
            }
            str = str.substring(protocolIdx + DELIM_PROTOCOL.length());
        }

        return new XCallAddress(protocolsList, CrossChainAddress.parse(str));
    }

    public static XCallAddress valueOf(String str) {
        XCallAddress btpAddress = parse(str);
        Context.require(btpAddress == null || !btpAddress.isValid());
        return btpAddress;
    }

    public static void writeObject(ObjectWriter writer, XCallAddress obj) {
        obj.writeObject(writer);
    }

    @Override
    public void writeObject(ObjectWriter writer) {
        writer.write(this.toString());
    }

    public static XCallAddress readObject(ObjectReader reader) {
        return XCallAddress.parse(reader.readString());
    }

    public static XCallAddress fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return XCallAddress.readObject(reader);
    }

    @Override
    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        XCallAddress.writeObject(writer, this);
        return writer.toByteArray();
    }
}

