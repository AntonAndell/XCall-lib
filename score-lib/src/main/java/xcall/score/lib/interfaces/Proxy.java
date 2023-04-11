package xcall.score.lib.interfaces;

import java.math.BigInteger;

import score.annotation.External;
import score.annotation.Optional;
import score.annotation.Payable;

public interface Proxy {
    @External(readonly = true)
    String getXCallNetworkAddress();

    @External
    void setProtocols(String network, String protocols);

    @External(readonly = true)
    String getProtocols(String network, String protocols);

    @Payable
    @External
    BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback);

    @External
    void handleCallMessage(String _from, byte[] _data);
}

