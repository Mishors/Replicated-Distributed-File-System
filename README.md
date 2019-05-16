# Replicated Distributed File System

The system consists of one main server (master) and, data will be replicated on multiple replicaServers. This file system allows its concurrent users to perform transactions, while guaranteeing ACID properties.

## General System Properties

- The master server maintains metadata about the replicas and their locations.
- The user can submit multiple operations that are performed atomically on the
- Files are not partitioned.
- Assumption: each transaction access only one file.
- Each file stored on the distributed file system has a primary replica. This means that sequential consistency is implemented through a protocol similar to the passive
(primary-backup) replication protocol.
- After performing a set of operations, it is guaranteed that the file system will remain in
consistent state.
- A lock manager is maintained at each replicaServer.
- Once any transaction is committed, its mutations to files stored in the file system are
required to be durable.

## Client specification

- Clients read and write data to the distributed file system.
- Each client has a file in its local directory that specifies the main server IP address.


## Master Server specification.

- The master server should communicate with clients through the given RMI interface.
- The master server need to be invoked using the following command. server -ip [ip address string] -port [port number] -dir <directory path>
where:
• **ip address** string is the ip address of the server. The default value is 127.0.0.1.
• **port number** is the port number at which the server will be listening to messages.The default is 8080.
- Assumption: The master server starts the replica servers while it is starting via SSH connections
