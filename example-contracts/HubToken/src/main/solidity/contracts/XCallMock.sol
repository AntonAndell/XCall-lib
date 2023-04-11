
// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0;
pragma abicoder v2;

import "./interfaces/ICallService.sol";
import "./interfaces/ICallServiceReceiver.sol";
import "./libraries/ParseAddress.sol";
import "./libraries/BTPAddress.sol";

contract XCallMock is ICallService {
    using ParseAddress for address;

    string private BtpAddress;

    constructor(string memory networkId) {
        BtpAddress = BTPAddress.btpAddress(networkId, address(this).toString());
    }

    function getBtpAddress(
    ) external override view returns (
        string memory
    ) {
        return BtpAddress;
    }

    function sendCallMessage(
        string memory _to,
        bytes memory _data,
        bytes memory _rollback
    ) external override payable returns (
        uint256
    ) {
        return 0;
    }

    function execute(
        string calldata _from,
        address _to,
        bytes calldata _data
    ) external {
         ICallServiceReceiver(_to).handleCallMessage(_from, _data);
    }

     function rollback(
        address _to,
        bytes calldata _data
    ) external {
         ICallServiceReceiver(_to).handleCallMessage(BtpAddress, _data);
    }

}