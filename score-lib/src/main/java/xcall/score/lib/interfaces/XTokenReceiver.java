package xcall.score.lib.interfaces;

import java.math.BigInteger;

import foundation.icon.score.client.ScoreInterface;

@ScoreInterface
public interface XTokenReceiver extends TokenReceiver {
     /**
    * Receives tokens cross chain enabled tokens where the from is in a ‘
    * String Address format,
    * pointing to an address on a XCall connected chain.
    *
    * Use BTPAddress as_from parameter?
    */
   void xTokenFallback(String _from, BigInteger _value, byte[] _data);
}