## TunnelTime
TunnelTime is a wrapper to make [JSch](http://www.jcraft.com/jsch/)
nicer to use from Scala.

### Example:

First, create a new ssh key and give it permission to tunnel through a
remote server by adding it to the `~/.ssh/authorized_keys` file.

```
no-pty,no-X11-forwarding,command="/bin/echo do-not-send-commands" ssh-rsa rsa-public-key-code-goes-here keyuser@keyhost
```

Consult the sshd man page for more options.

Then, with some commands like this:

```scala
  import me.chadrs.tunneltime.{KeyAuthentication, TunnelHost, Tunnel}
  val myauth = KeyAuthentication("chad", "/Users/chadrs/.ssh/tunneling_id_rsa")
  val host = TunnelHost(myauth, "host.to.tunnel.through"),
  Tunnel.tunnelRemoteAddrToLocalPort(host, "example.org", 80, 10080)
```

You should now be able to open a connection to "example.com" on port 80
when making connections to localhost:10080, all going through
your tunnel.
