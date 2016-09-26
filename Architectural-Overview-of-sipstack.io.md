Architectural Overview of sipstack.io
Sipstack.io provides a fast and reliable SIP framework whose primary purpose is to provide developers with a stack that is lightweight, ops-friendly and optimized for production deployment out-of-the-box. It comes with sophisticated configuration, application metrics and operational tools, allowing you and your team to build a production ready SIP service in the shortest amount of time possible. [Netty.io](http://netty.io/) is providing the raw network stack and [pkts.io](http://www.aboutsip.com/pktsio) is the library used for framing and parsing of SIP messages. 

This document describes the design philosophy and implementation of sipstack.io.

# General design philosophy

In short, the design philosophy of sipstack.io is:

- All objects are immutable.
- All IO operations are asynchronous.
- All connections are modeled as Flows and a flow is a central concept.
- Connections and connection management is a first class citizen:
  - Connection management is part of the core and all network operations takes place across so-called Flows.
  - All network events are guaranteed to be processed in the order they arrived on the machine.
- State machines:
  - Everything that can be modeled as a state machine is implemented as state machine (dialog, transaction, connection flow etc).
  - All state machines only handle a single event at a time and all other events targeting the same state machine will be queued up and processed in the order they were received.
- Like SIP, this implementation is layered where each layer has a distinct function and each layer accepts only one type of event (e.g. IOEvents) and typically will emit another, higher-level, event (e.g. SipMessageEvent)
## Layers

SIP is layered protocol and consists of a fairly distinct set of processing steps, each represented as a layer and [sipstack.io](http://www.sipstack.io/) is implemented the same way, which also follows the model of [netty.io](http://www.netty.io) very well. 

**The Syntax Layer**
The syntax layer is responsible for ensuring that the incoming raw bytes across a socket actually is a proper SIP Message. sipstack.io is using [siplib.io](http://www.siplib.io/) for all its framing and parsing logic.

**The Transport Layer**
As stated by [RFC3261](https://www.ietf.org/rfc/rfc3261.txt), the transport layer is responsible for the actual transmissions of SIP requests and responses over the network and is also responsible for managing connections. However, apart from the basic functionality of connection management as outlined by RFC3261, sipstack.io has support for “sip outbound” ([RFC5626](https://tools.ietf.org/html/rfc5626)) out-of-the-box and has made “connection management” a core concept in the stack. In fact, every network operation (e.g. sending & receiving) will take place across a “flow”, which represents a bi-directional stream of data between two network endpoints. A flow can be equal to that of a TCP connection, or it can represent the flow of UDP packets between two hosts.

Furthermore, a flow is implemented as a finite state machine (FSM) 

**The Transaction Layer**
From RFC3261:

     *SIP is a transactional protocol: interactions between components take
*  *   place in a series of independent message exchanges.  Specifically, a
*  *   SIP transaction consists of a single request and any responses to
*  *   that request, which include zero or more provisional responses and
*  *   one or more final responses. *

sipstack.io is of course implementing the transaction layer according to RFC3261 but it also has support for [RFC6026](https://tools.ietf.org/rfc/rfc6026) from the get-go, which is a small but important update to the transaction state machine outlined by the original RFC.

**The Transaction User Layer**
All layers above the Transaction Layer are referred to as a Transaction User and sipstack.io is implementing the standard ones, which are:

- Proxy Core
- UA Core

