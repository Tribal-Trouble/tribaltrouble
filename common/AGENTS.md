# common/ notes for agents

Read the root [AGENTS.md](../AGENTS.md) first. This module is shared by the client and the
live server, so it is the highest-risk place to change.

## The wire boundary

`com.oddlabs.matchmaking`, `com.oddlabs.router`, and `com.oddlabs.registration` hold the
interfaces the client and server call across the network. They are dispatched by ARMI
(`com.oddlabs.net`), which serializes a method by its position in the sorted signature list,
not by name. Consequences:

- Adding, removing, renaming, or reordering a method on a wire interface changes every
  method id after it. The client and server must be rebuilt and deployed together.
- Changing a parameter type or order is the same kind of break.
- Adding a field to a class serialized across the boundary is a break unless the serializer
  handles it explicitly.

The `api-guard` CI job fails on any such edit that is not paired with an `API_VERSION` bump
in `util/Compatibility.java` or a maintainer's `[api-compat-ack]` commit. Do not bump
`API_VERSION` yourself; flag that it is needed.

## Determinism support

`com.oddlabs.event` and `util/DeterministicSerializer` back the event log that makes replay
and desync investigation possible. `Deterministic.log(...)` wraps values that differ per
machine so replay can feed the recorded value back. If you touch this code, understand that
a recording and its replay must see the exact same sequence of logged values.

## Networking

`com.oddlabs.net` is hand-rolled NIO with a selector thread, a task thread, and blocking
tasks queued onto it. It is old and subtle. Prefer small, local changes with a clear reason
over restructuring.
