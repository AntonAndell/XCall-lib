package icon.xcall.lib.util;

import score.ByteArrayObjectWriter;
import score.Context;
import score.ObjectReader;
import score.ObjectWriter;
public class CrossChainAddress {
    protected static final String DELIM_NET="/";
    protected static final String NATIVE = "Native";
    protected String net;
    protected String account;


    public CrossChainAddress(String net, String account) {
        this.net = net;
        this.account = account;
    }

    public CrossChainAddress(String account) {
        this.account = account;
        this.net = NATIVE;
    }

    public String net() {
        return net;
    }

    public String account() {
        return account;
    }

    public boolean isNative() {
        return net.equals(NATIVE);
    }
    @Override
    public String toString() {
        return net + DELIM_NET + account;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CrossChainAddress that = (CrossChainAddress) o;
        return toString().equals(that.toString());
    }

    @Override
    public int hashCode() {
        return toString().hashCode();
    }

    public boolean isValid() {
        return   (!(account == null || account.isEmpty()));
    }

    public static CrossChainAddress parse(String str) {
        if (str == null) {
            return null;
        }
        String net = "";
        String address = "";

         int netIdx = str.indexOf(DELIM_NET);
        if (netIdx >= 0) {
            net = str.substring(0, netIdx);
            address = str.substring(netIdx+DELIM_NET.length());
        } else {
            address = str;
        }
        return new CrossChainAddress(net, address);
    }

    public static CrossChainAddress valueOf(String str) {
        CrossChainAddress btpAddress = parse(str);
        Context.require(btpAddress == null || !btpAddress.isValid());
        return btpAddress;
    }

    public static void writeObject(ObjectWriter writer, CrossChainAddress obj) {
        obj.writeObject(writer);
    }

    public void writeObject(ObjectWriter writer) {
        writer.write(this.toString());
    }

    public static CrossChainAddress readObject(ObjectReader reader) {
        return CrossChainAddress.parse(reader.readString());
    }

    public static CrossChainAddress fromBytes(byte[] bytes) {
        ObjectReader reader = Context.newByteArrayObjectReader("RLPn", bytes);
        return CrossChainAddress.readObject(reader);
    }

    public byte[] toBytes() {
        ByteArrayObjectWriter writer = Context.newByteArrayObjectWriter("RLPn");
        CrossChainAddress.writeObject(writer, this);
        return writer.toByteArray();
    }
}

