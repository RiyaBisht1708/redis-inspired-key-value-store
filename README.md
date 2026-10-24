# Redis-Inspired Key-Value Store in Java

## Overview

A Java implementation of a Redis-inspired in-memory key-value store. This project explores how a TCP server receives client commands, processes them, stores values, and sends responses using the Redis Serialization Protocol (RESP).

The project builds on a CodeCrafters Redis challenge repository, with additional implementation, debugging, and testing performed during development.

## Features

The following commands have been manually tested:

- **PING** — responds with a PONG message.
- **SET** — stores a key-value pair.
- **GET** — retrieves a stored value.
- **DEL** — deletes keys and returns the number of keys deleted.

The server also handles commands split across multiple TCP reads, as verified during development.

## Tech Stack

- Java
- Maven
- TCP sockets
- Redis Serialization Protocol (RESP)
- JUnit and configured testing dependencies

## Getting Started

### Prerequisites

- A compatible Java Development Kit
- Maven

### Run the server

Clone the repository and navigate to the project directory:

```bash
git clone <your-repository-url>
cd <your-repository-directory>
```

Start the server:

```bash
./your_program.sh
```

The server uses port `6379` by default. Make sure the port is available.

### Run tests

```bash
mvn clean test
```

## Example

With the server running, connect using a TCP client such as `nc`:

```bash
nc localhost 6379
```

Send RESP-formatted commands. For example, the following request represents `PING`:

```text
*1
$4
PING
```

The expected response is:

```text
+PONG
```

Other manually tested commands include `SET`, `GET`, and `DEL`.

## What I Learned

- Handling TCP client connections in Java.
- Understanding RESP request and response formatting.
- Implementing in-memory key-value storage operations.
- Debugging command parsing and server lifecycle issues.
- Testing server behavior with command-line TCP clients.
- Handling incomplete commands received across multiple TCP reads.

## Acknowledgements

This project builds on the CodeCrafters Redis challenge repository. Please refer to the original repository and its applicable license for attribution and usage terms.
