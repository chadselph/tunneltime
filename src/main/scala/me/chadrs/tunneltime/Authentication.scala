package me.chadrs.tunneltime

/**
 * Base class for authentication strategies
 * @param user Username
 */
sealed abstract class Authentication(val user: String)

/**
 * Authentication with a `keyFile` (like ~/.ssh/id_rsa)
 * @param user Username
 * @param keyFile Path to identity file
 * @param passphrase Passphrase on the identify file, or None if no passphrase.
 */
case class KeyAuthentication (override val user: String, keyFile: String = ".ssh/id_rsa", passphrase: Option[String] = None) extends Authentication(user)

/**
 * Password based authentication (not recommended)
 * @param user Username
 * @param password Your password. To avoid having passwords in source code, consider using
 *                 [[me.chadrs.tunneltime.KeyAuthentication]].
 */
case class PasswordAuthentication (override val user: String, password: String) extends Authentication(user)
