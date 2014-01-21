package me.chadrs.tunneltime

import scala.collection.mutable
import com.jcraft.jsch.{Session, JSch, UserInfo}
import org.slf4j.LoggerFactory
import java.net.InetSocketAddress
import scala.util.Try


object Tunnel {

  val CONNECT_TIMEOUT = 10 * 1000
  private val jsch = new JSch()
  private val logger = LoggerFactory.getLogger(getClass)

  private def makeSession (th: TunnelHost) = {
    val session = jsch.getSession(th.auth.user, th.hostname, th.port)

    th.auth match {
      case KeyAuthentication(_, keyFile, None) => jsch.addIdentity(keyFile)
      case KeyAuthentication(_, keyFile, Some(pp)) => jsch.addIdentity(keyFile, pp)
      case _ =>
    }
    session.setUserInfo(new UserInfo {

      def promptPassword(p1: String): Boolean = true
      def promptPassphrase(p1: String): Boolean = true

      def promptYesNo(s: String): Boolean = {
        /*
        Would really like to fix this... also would really like to not have to parse a string to determine
        what kind of prompt this is. Ideally, there could be a settings file with options like...
        Ok to replace key file? Y/N
        Ok to create new file? Y/N
        */
        logger.warn(s)
        true
      }

      def showMessage(s: String) {
        logger.info(s)
      }

      def getPassword: String = th.auth match {
        case PasswordAuthentication(_, pass) => pass
        case _ => null
      }

      def getPassphrase: String = th.auth match {
        case KeyAuthentication(_, _, Some(passphrase)) => passphrase
        case _ => null
      }
    })
    session.connect(CONNECT_TIMEOUT)
    session

  }

  private def getOrCreateTunnel(session: Session, remoteHost: String, remotePort: Int, localPort: Int = 0): Int = {
    session.getPortForwardingL.filter(_.endsWith(s"$remoteHost:$remotePort")).toList match {
      case Nil => session.setPortForwardingL(localPort, remoteHost, remotePort)
      case first :: _ => first.split(":")(0).toInt
    }
  }

  val activeTunnels:mutable.Map[TunnelHost, Session] = mutable.Map()

  /**
   * Convinence function for using InetSocketAddress instead of separate host and port params.
   */
  def tunnelRemoteAddrToLocalPort(tunnelHost: TunnelHost, rAddress: InetSocketAddress): Int =
    tunnelRemoteAddrToLocalPort(tunnelHost, rAddress.getHostName, rAddress.getPort, 0)
  /**
   * Convinence function for using InetSocketAddress instead of separate host and port params.
   */
  def tunnelRemoteAddrToLocalPort(tunnelHost: TunnelHost, rAddress: InetSocketAddress, lPort: Int): Int = {
    tunnelRemoteAddrToLocalPort(tunnelHost, rAddress.getHostName, rAddress.getPort, lPort)
  }

  /**
   * Tunnel to a remote address (`rhost` and `rport`) through some `tunnelHost`
   * @param tunnelHost Host to run the tunnel through. Connect will be created if none exists,
   *                   otherwise it will reuse an existing ssh connection.
   * @param rhost Remote hostname (or IP address)
   * @param rPort Remote port number
   * @param lPort The local port on which to listen for traffic and forward it through the
   *              tunnel to the remote host/port. Defaults to random high port.
   * @return Returns the port number being listened on.
   */
  def tunnelRemoteAddrToLocalPort(tunnelHost: TunnelHost, rhost: String, rPort: Int, lPort: Int = 0): Int = {
    activeTunnels.get(tunnelHost) match {
      case Some(sess) =>
        if (sess.isConnected) {
          getOrCreateTunnel(sess, rhost, rPort, lPort)
        } else {
          activeTunnels.remove(tunnelHost)
          tunnelRemoteAddrToLocalPort(tunnelHost, rhost, rPort, lPort)
        }
      case None =>
        activeTunnels(tunnelHost) =  makeSession(tunnelHost)
        tunnelRemoteAddrToLocalPort(tunnelHost, rhost, rPort, lPort)
    }
  }


  def removeReverseTunnel(tunnelHost: TunnelHost, rPort: Int): Unit = {
    activeTunnels.get(tunnelHost) match {
      case Some(sess) => {
        if(sess.isConnected) {
          sess.delPortForwardingR(rPort)
        } else {
          activeTunnels.remove(tunnelHost)
        }
      }
      case None => {}
    }
  }

  def getOrCreateReverseTunnel(session: Session, lHost: String, lPort: Int, rHost: String, rPort: Int): Unit = {
    session.delPortForwardingR(rPort)
    session.setPortForwardingR(rHost, rPort, lHost, lPort)
  }


  def executeWithReverseTunnel[T](tunnelHost: TunnelHost, lHost: String, lPort: Int, rHost: String, rPort: Int)(run: => T): T = {
    val sess = makeSession(tunnelHost)
    getOrCreateReverseTunnel(sess, lHost, lPort, rHost, rPort)
    val result = run
    sess.disconnect()
    result
  }

  def tunnelLocalAddrToRemotePort(tunnelHost: TunnelHost, lHost: String, lPort: Int, rHost: String, rPort: Int): Unit = {
    activeTunnels.get(tunnelHost) match {
      case Some(sess) =>
        if (sess.isConnected) {
          Try(getOrCreateReverseTunnel(sess, lHost, lPort, rHost, rPort)).getOrElse({
            activeTunnels.remove(tunnelHost)
            tunnelLocalAddrToRemotePort(tunnelHost, lHost, lPort, rHost, rPort)
          })
        } else {
          activeTunnels.remove(tunnelHost)
          tunnelLocalAddrToRemotePort(tunnelHost, lHost, lPort, rHost, rPort)
        }
      case None => {
        activeTunnels(tunnelHost) = makeSession(tunnelHost)
        tunnelLocalAddrToRemotePort(tunnelHost, lHost, lPort, rHost, rPort)
      }
    }
  }
}
