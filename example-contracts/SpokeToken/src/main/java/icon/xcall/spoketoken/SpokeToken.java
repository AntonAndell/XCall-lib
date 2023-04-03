package icon.xcall.spoketoken;

import score.annotation.External;
import score.annotation.Optional;

import java.math.BigInteger;
import icon.xcall.lib.annotation.XCall;
import com.iconloop.score.token.irc2.IRC2;

public interface SpokeToken extends IRC2 {
    /**
     * Returns the account balance of another account with string address {@code _owner}, 
     * which can be both ICON and BTP Address format.
     */
    @External(readonly = true)
    BigInteger xBalanceOf(String _owner);

     /**
     * If {@code _to} is a ICON address, use IRC2 transfer
     * Transfers {@code _value} amount of tokens to BTP address {@code _to}, 
     * and MUST fire the {@code HubTransfer} event.
     * This function SHOULD throw if the caller account balance does not have enough tokens to spend.
     */
    @External
    void hubTransfer(String _to, BigInteger _value, @Optional byte[] _data);

    /**
     * Callable only via XCall service on ICON.
     * Transfers {@code _value} amount of tokens to address {@code _to}, 
     * and MUST fire the {@code HubTransfer} event.
     * This function SHOULD throw if the caller account balance does not 
     * have enough tokens to spend.
     * If {@code _to} is a contract, this function MUST invoke the function 
     * {@code xTokenFallback(String, int, bytes)}
     * in {@code _to}. If the {@code xTokenFallback} function is not implemented 
     * in {@code _to} (receiver contract),
     * then the transaction must fail and the transfer of tokens should not occur.
     * If {@code _to} is an externally owned address, then the transaction
     * must be sent without trying to execute
     * {@code XTokenFallback} in {@code _to}.
     * {@code _data} can be attached to this token transaction.
     * {@code _data} can be empty.
     */
    @XCall
    void hubTransfer(String from, String _to, BigInteger _value, byte[] _data);

    /**
     * (EventLog) Must trigger on any successful hub token transfers.
     */
    void HubTransfer(String _from, String _to, BigInteger _value, byte[] _data);
}