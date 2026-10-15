package com.nexvary.trafficlab.vpn;
/** All values originate from the local gateway, never from arbitrary YAML input. */
public final class TunnelConfig {
    private TunnelConfig(){}
    public static String create(int port,String user,String password){
        if(port<1||port>65535||!user.matches("[A-Za-z0-9_-]+")||!password.matches("[A-Za-z0-9_-]+"))throw new IllegalArgumentException("Invalid gateway configuration");
        return "tunnel:\n  name: nexvary\n  mtu: 1500\n  ipv4: 198.18.0.1\n  ipv6: 'fd77::1'\n"
            +"socks5:\n  address: 127.0.0.1\n  port: "+port+"\n  username: '"+user+"'\n  password: '"+password+"'\n  udp: 'udp'\n"
            +"mapdns:\n  address: 198.18.0.2\n  port: 53\n  network: 100.64.0.0\n  netmask: 255.192.0.0\n  cache-size: 1024\n"
            +"misc:\n  max-session-count: 128\n  connect-timeout: 10000\n  tcp-read-write-timeout: 180000\n  udp-read-write-timeout: 60000\n  log-level: error\n  log-file: stderr\n";
    }
}
