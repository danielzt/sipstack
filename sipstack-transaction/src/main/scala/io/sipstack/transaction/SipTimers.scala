/**
 *
 */
package io.sipstack.transaction

/**
 * 
 * From rfc3261 page 264
 * 
 * <pre>
 * 
 * A Table of Timer Values
 *
 * Table 4 summarizes the meaning and defaults of the various timers
 * used by this specification.
 * 
 * Timer    Value            Section               Meaning
 * ----------------------------------------------------------------------
 * T1       500ms default    Section 17.1.1.1     RTT Estimate
 * T2       4s               Section 17.1.2.2     The maximum retransmit
 *                                                interval for non-INVITE
 *                                                requests and INVITE
 *                                                responses
 * T4       5s               Section 17.1.2.2     Maximum duration a
 *                                                message will
 *                                                remain in the network
 * Timer A  initially T1     Section 17.1.1.2     INVITE request retransmit
 *                                             interval, for UDP only
 * Timer B  64*T1            Section 17.1.1.2     INVITE io.sipstack.transaction.transaction
 *                                             timeout timer
 * Timer C  > 3min           Section 16.6         proxy INVITE io.sipstack.transaction.transaction
 *                         bullet 11            timeout
 * Timer D  > 32s for UDP    Section 17.1.1.2     Wait time for response
 *       0s for TCP/SCTP                       retransmits
 * Timer E  initially T1     Section 17.1.2.2     non-INVITE request
 *                                                retransmit interval,
 *                                                UDP only
 * Timer F  64*T1            Section 17.1.2.2     non-INVITE io.sipstack.transaction.transaction
 *                                                timeout timer
 * Timer G  initially T1     Section 17.2.1       INVITE response
 *                                                retransmit interval
 * Timer H  64*T1            Section 17.2.1       Wait time for
 *                                                ACK receipt
 * Timer I  T4 for UDP       Section 17.2.1       Wait time for
 *          0s for TCP/SCTP                       ACK retransmits
 * Timer J  64*T1 for UDP    Section 17.2.2       Wait time for
 *          0s for TCP/SCTP                       non-INVITE request
 *                                                retransmits
 * Timer K  T4 for UDP       Section 17.1.2.2     Wait time for
 *          0s for TCP/SCTP                       response retransmits
 * 
 * </pre>
 * 
 * @author jonas@jonasborjesson.com
 */
object SipTimers {
  
  sealed trait SipTimer {
    def time:Long
  }
  
  case class TimerA(time:Long) extends SipTimer
  case class TimerB(time:Long) extends SipTimer
  case class TimerC(time:Long) extends SipTimer
  case class TimerD(time:Long) extends SipTimer
  case class TimerE(time:Long) extends SipTimer
  case class TimerF(time:Long) extends SipTimer
  case class TimerG(time:Long) extends SipTimer
  case class TimerH(time:Long) extends SipTimer
  case class TimerI(time:Long) extends SipTimer
  case class TimerJ(time:Long) extends SipTimer
  case class TimerK(time:Long) extends SipTimer

}