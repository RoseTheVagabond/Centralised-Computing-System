# Centralized Computing System (CCS)

A Java-based concurrent server application that provides arithmetic computation services with service discovery capability.

## Overview

The Centralized Computing System (CCS) is a server application that provides the following functionalities:

1. **Service Discovery** - Uses UDP to respond to discovery requests, allowing clients to locate the server on a network
2. **Computation Services** - Processes arithmetic operations (ADD, SUB, MUL, DIV) requested by clients via TCP
3. **Statistics Reporting** - Tracks and periodically reports usage statistics

## Features

- UDP-based service discovery responding to "CCS DISCOVER" messages
- TCP-based server handling client computation requests
- Support for multiple concurrent clients
- Thread-safe operation counters and statistics
- Periodic (10-second interval) statistics reporting
- Error handling for malformed requests and invalid operations

## Requirements

- Java 8 (JDK 1.8)
- No external libraries required (uses only standard Java libraries)

## Building the Project

To compile the project, use:
```
javac CCS.java
```
To create a JAR file:
```
jar cvfe CCS.jar CCS CCS.class CCS$ClientHandler.class
```

## Running the server

To run the CCS server:
```
java -jar CCS.jar <port>
```
Where `<port>` is the port number to use for both UDP discovery and TCP client connections.

## Protocol Specification

### Service Discovery (UDP)

- Client sends: `CCS DISCOVER` (broadcast message)
- Server responds: `CCS FOUND`

### Computation Protocol (TCP)

Request format:
```
<OPER> <ARG1> <ARG2>
```
Where:
- `<OPER>` is one of: ADD, SUB, MUL, DIV
- `<ARG1>` and `<ARG2>` are integer values

Response format:
- A single line containing the computed integer result
- Or the text `ERROR` if the operation cannot be performed

Examples:
- Request: `ADD 5 3` → Response: `8`
- Request: `SUB 10 4` → Response: `6`
- Request: `MUL 3 7` → Response: `21`
- Request: `DIV 20 5` → Response: `4`
- Request: `DIV 10 0` → Response: `ERROR`

## Statistics Tracking

The server maintains and reports the following statistics:

### Total (since server start):
- Connected clients count
- Total requests processed
- Count of ADD operations
- Count of SUB operations
- Count of MUL operations
- Count of DIV operations
- Count of error operations
- Sum of all computed values

### Periodic (last 10 seconds):
- New client connections
- Requests processed
- Count of ADD operations
- Count of SUB operations
- Count of MUL operations
- Count of DIV operations
- Count of error operations
- Sum of computed values

## Implementation Details

The CCS implementation uses:

- `ServerSocket` for accepting TCP client connections
- `DatagramSocket` for UDP service discovery
- Thread pool (`ExecutorService`) for handling concurrent client connections
- Atomic variables for thread-safe statistics tracking
- `ConcurrentHashMap` to maintain active client connections

## Error Handling

The server handles the following error conditions:

- Invalid port number
- Invalid operation requests
- Malformed requests (incorrect format or number of arguments)
- Division by zero
- Client disconnection

## Limitations

- Works only on local network for discovery (UDP broadcast)
- Limited to integer arithmetic operations
