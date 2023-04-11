// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;
pragma abicoder v2;

import "./RLPDecode.sol";
import "./Types.sol";

library RLPDecodeStruct {
    using RLPDecode for RLPDecode.RLPItem;
    using RLPDecode for RLPDecode.Iterator;
    using RLPDecode for bytes;

    using RLPDecodeStruct for bytes;

    uint8 private constant LIST_SHORT_START = 0xc0;
    uint8 private constant LIST_LONG_START = 0xf7;

    function decodeCrossTransfer(RLPDecode.RLPItem[] memory ls)
        internal
        pure
        returns (Types.CrossTransfer memory)
    {
        return
            Types.CrossTransfer(
                string(ls[1].toBytes()),
                string(ls[2].toBytes()),
                ls[3].toUint(),
                ls[4].toBytes()
            );
    }

    function decodeCrossTransferRevert(RLPDecode.RLPItem[] memory ls)
        internal
        pure
        returns (Types.CrossTransferRevert memory)
    {
        return
            Types.CrossTransferRevert(
                string(ls[1].toBytes()),
                ls[2].toUint()
            );
    }
}
