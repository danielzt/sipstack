package io.sipstack.transaction

import io.pkts.packet.sip.SipMessage
import io.pkts.packet.sip.impl.PreConditions
import java.nio.charset.Charset
import io.netty.util.CharsetUtil

/**
 * @author jonas@jonasborjesson.com
 */
final object TransactionId {
  
  private val US_ASCII:Charset = Charset.forName("US-ASCII")
  
  def create(msg:SipMessage): TransactionId = {
    PreConditions.ensureNotNull(msg, "SIP message cannot be null")
    val via = msg.getViaHeader
    PreConditions.ensureNotNull(via, "No Via-header found in the SIP message")
    val branch = via.getBranch
    val capacity = branch.capacity
    val length = if (msg.isCancel) capacity + 7 else capacity
    val id = new Array[Byte](length)
    branch.getByes(id)
    if (msg.isCancel) {
        id(capacity + 0) = '-'
        id(capacity + 1) = 'C'
        id(capacity + 2) = 'A'
        id(capacity + 3) = 'N'
        id(capacity + 4) = 'C'
        id(capacity + 5) = 'E'
        id(capacity + 6) = 'L'

    }
    TransactionId(new String(id, US_ASCII))
  }

}

case class TransactionId(id:String)


