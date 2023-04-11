package xcall.score.lib.interfaces;

import java.math.BigInteger;

import foundation.icon.score.client.ScoreInterface;
import score.annotation.External;
import score.annotation.Optional;

@ScoreInterface
public interface XCall {
    @External
    public BigInteger sendCallMessage(String _to, byte[] _data, @Optional byte[] _rollback);
}
