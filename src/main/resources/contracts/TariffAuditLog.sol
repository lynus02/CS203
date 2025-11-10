// SPDX-License-Identifier: MIT
pragma solidity ^0.8.0;

contract TariffAuditLog {
    struct AuditEntry {
        string dbHash;
        uint256 timestamp;
        address uploader;
    }

    AuditEntry[] public logs;

    event HashStored(string dbHash, address indexed uploader, uint256 timestamp);

    function storeHash(string memory _dbHash) public {
        logs.push(AuditEntry(_dbHash, block.timestamp, msg.sender));
        emit HashStored(_dbHash, msg.sender, block.timestamp);
    }

    function getLatestHash() public view returns (string memory) {
        require(logs.length > 0, "No hashes stored");
        return logs[logs.length - 1].dbHash;
    }
}