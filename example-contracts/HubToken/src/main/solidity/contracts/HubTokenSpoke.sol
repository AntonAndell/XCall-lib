
// SPDX-License-Identifier: MIT
pragma solidity >=0.8.0;
pragma abicoder v2;

import "./interfaces/ICallService.sol";
import "./interfaces/ICallServiceReceiver.sol";
import "./libraries/RLPDecode.sol";
import "./libraries/RLPDecodeStruct.sol";
import "./libraries/RLPEncodeStruct.sol";
import "./libraries/Types.sol";
import "./libraries/BTPAddress.sol";
import "./libraries/ParseAddress.sol";

import "@openzeppelin/contracts/token/ERC20/ERC20.sol";

contract HubTokenSpoke is ERC20 {
    using RLPDecode for bytes;
    using RLPDecode for RLPDecode.RLPItem;

    using RLPEncodeStruct for Types.CrossTransfer;
    using RLPEncodeStruct for Types.CrossTransferRevert;

    using ParseAddress for address;
    using ParseAddress for string;

    address public xCall;
    string public xCallBTPAddress;
    string public nid;
    string public hubAddress;
    string public hubNet;
    address owner;

    constructor(address _xCall, string memory _hubAddress) ERC20("HubToken", "HUBT") {
        xCall = _xCall;
        xCallBTPAddress = ICallService(xCall).getBtpAddress();
        (nid, ) = BTPAddress.parseBTPAddress(xCallBTPAddress);
        (hubNet, hubAddress) = BTPAddress.parseNetworkAddress(_hubAddress);
        owner = msg.sender;
    }

    modifier onlyCallService() {
        require(msg.sender == xCall, "OnlyCallService");
        _;
    }

    modifier onlyOwner() {
        require(msg.sender == owner);
        _;
    }

    function compareTo(
        string memory _base,
        string memory _value
    ) internal pure returns (bool) {
        if (
            keccak256(abi.encodePacked(_base)) ==
            keccak256(abi.encodePacked(_value))
        ) {
            return true;
        }
        return false;
    }


    function setup(
        address _xCall, string memory _hubAddress
    ) external onlyOwner {
        xCall = _xCall;
        xCallBTPAddress = ICallService(xCall).getBtpAddress();
        (nid, ) = BTPAddress.parseBTPAddress(xCallBTPAddress);
        (hubNet, hubAddress) = BTPAddress.parseNetworkAddress(_hubAddress);
    }

    function handleCallMessage(
        string calldata _from,
        bytes calldata _data
    ) external onlyCallService {
        RLPDecode.RLPItem[] memory data = _data.toRlpItem().toList();
        string memory method = string(data[0].toBytes());
        if (compareTo(method, "xCrossTransfer")) {
            xCrossTransfer(_from, RLPDecodeStruct.decodeCrossTransfer(data));
        } else if (compareTo(method, "xCrossTransferRevert")) {
            xCrossTransferRevert(_from, RLPDecodeStruct.decodeCrossTransferRevert(data));
        }
    }

    function crossTransfer(string calldata to, uint256 amount, bytes calldata data) external payable {
        _burn(msg.sender, amount);
        string memory from = BTPAddress.btpAddress(nid, msg.sender.toString());
        Types.CrossTransfer memory callData = Types.CrossTransfer(
            from,
            to,
            amount,
            data
        );

        Types.CrossTransferRevert memory rollbackData = Types.CrossTransferRevert(
            from,
            amount
        );
        ICallService(xCall).sendCallMessage{value:msg.value} (
            BTPAddress.btpAddress(hubNet, hubAddress),
            callData.encodeCrossTransferMessage(),
            rollbackData.encodeCrossTransferRevertMessage()
        );
    }

    // Currently allows no contract interactions
    function xCrossTransfer(string memory from, Types.CrossTransfer memory crossTransferData) internal {
        require(compareTo(from, BTPAddress.btpAddress(hubNet, hubAddress)), "OnlyHub");
        (string memory net, string memory account) = BTPAddress.parseNetworkAddress(crossTransferData.to);
        require(compareTo(net, nid), "Wrong Network");

        address to = ParseAddress.parseAddress(account, "Invalid to Address");
        _mint(to, crossTransferData.value);
        // TODO Emit log
    }

    function xCrossTransferRevert(string calldata _from, Types.CrossTransferRevert memory crossTransferRevertData) internal {
        require(compareTo(_from, xCallBTPAddress), "OnlyCallService");
        (string memory net, string memory account) = BTPAddress.parseNetworkAddress(crossTransferRevertData.from);
        require(compareTo(net, nid), "Wrong Network");
        address to = ParseAddress.parseAddress(account, "Invalid to Address");
        _mint(to, crossTransferRevertData.value);
    }
}