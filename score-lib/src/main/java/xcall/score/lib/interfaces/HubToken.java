package xcall.score.lib.interfaces;

import java.math.BigInteger;

import icon.xcall.lib.annotation.XCall;
import score.annotation.EventLog;
import score.annotation.External;

public interface HubToken extends SpokeToken {
    /**
    * Returns the total token supply across all connected chains.
    */
    @External(readonly = true)
    BigInteger xTotalSupply();

     /**
    * Returns the total token supply on a connected chain.
    */
    @External(readonly = true)
    BigInteger xSupply(String spokeAddress);

    /**
    * Returns a list of all contracts across all connected chains
    */
    @External(readonly = true)
    String[] getConnectedChains();

    /**
     * If {@code _to} is a ICON address, use IRC2 transfer
     * If {@code _to} is a BTPAddress, then the transaction must
     * trigger xTransfer via XCall on corresponding spoke chain
     * and MUST fire the {@code XTransfer} event.
     * {@code _data} can be attached to this token transaction.
     * {@code _data} can be empty.
     * XCall rollback message is specified to match {@link #xTransferRevert}.
     */
    @External
    void crossTransfer(String _to, BigInteger _value, byte[] _data);

    /**
     * XCall version of xTransfer.
     * If {@code _to} is a contract trigger xTokenFallback(String, int, byte[])
     * instead of regular tokenFallback.
     * Internal behavior same as {@link #xTransfer} but from parameters is specified by
     * XCall rather than the blockchain.
     */
    @XCall
    void xCrossTransfer(String from, String _from, String _to, BigInteger _value, byte[] _data);

    @XCall
    void xCrossTransferRevert(String from, String _to, BigInteger _value);

      /**
     * From is a EOA address of a connected chain
     * Uses From to xTransfer the balance on ICON to native address on calling chain.
     */
    @XCall
    void xWithdraw(String from, BigInteger _value);

    /**
     * (EventLog) Must trigger on any successful token transfers from cross chain addresses.
     */
    @EventLog(indexed = 1)
    void XTransfer(BigInteger id, String _from, String _to, BigInteger _value, byte[] _data);
}

