package me.chadrs.tunneltime

/**
 * TunnelHosts represent hosts you can connect to over ssh.
 * @param auth How you authenticate to this host
 * @param hostname Hostname
 * @param publicKeySignature Hosts signature. You should probably set this to prevent
 *                           man-in-the-middle attacks.
 * @param port Port ssh is listening on (probably 22)
 */
case class TunnelHost(auth: Authentication, hostname: String, publicKeySignature: Option[String] = None, port: Int = 22)

