// SPDX-License-Identifier: Apache-2.0
pragma solidity >=0.8.0;

/**
 * @notice List of ALL Struct being used to Encode and Decode RLP Messages
 */
library Types {
    struct CrossTransfer {
        string from;
        string to;
        uint256 value;
        bytes data;
    }

    struct CrossTransferRevert {
        string from;
        uint256 value;
    }
}
