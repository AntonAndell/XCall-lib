package xcall.score.lib.interfaces;

import java.math.BigInteger;

import score.Address;

public interface TokenReceiver {
   void tokenFallback(Address _from, BigInteger _value, byte[] _data);
}