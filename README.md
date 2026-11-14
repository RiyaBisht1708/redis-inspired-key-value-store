# Redis-Inspired Key-Value Store (Java)

A Redis-compatible in-memory server written in Java. It speaks RESP over TCP, so
standard tools like `redis-cli` and `redis-benchmark` work against it. Built from the
CodeCrafters "Build your own Redis" challenge, then extended with my own debugging,
tests, and fixes.

## Features
- Commands: PING, ECHO, SET (with PX expiry), GET, DEL, INCR, INFO
- Transactions: MULTI / EXEC / DISCARD
- Replication: master and replica modes (`--replicaof`), REPLCONF, PSYNC, WAIT
- RESP parser that handles commands split across TCP reads and counts Unicode by bytes
- Thread-safe store (ConcurrentHashMap + read-write lock); one virtual thread per client

## Run
Requires JDK 24+ and Maven.

    ./your_program.sh --port 6380
    redis-cli -p 6380 SET a 1 PX 2000
    redis-cli -p 6380 GET a

## Test
    mvn clean test

## Benchmark
`redis-benchmark -p 6380 -t set,get -n 100000 -c 50 -q`
on a MacBook Air, localhost, 50 clients:

| Command | Requests/s | p50 latency |
|---|---|---|
| SET | ~130K | 0.2 ms |
| GET | ~165K | 0.17 ms |

## Bugs found and fixed
- Shared thread pool starved clients: 20 idle connections blocked new ones. Fixed with
  virtual threads.
- PX expiry was case-sensitive, so `PX` from redis-cli was ignored. Fixed.
- Unknown commands returned no reply, which hung clients. They now return an error.

## Known limitations
- INCR is not yet atomic under concurrent clients.
- Persistence is not implemented.