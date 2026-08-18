# Print IPv4 addresses that look like LAN unicast (skip 127.0.0.1).
Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue |
    Where-Object {
        $_.IPAddress -notlike "127.*" -and
        $_.PrefixOrigin -ne "WellKnown"
    } |
    Select-Object -Property IPAddress, InterfaceAlias |
    Format-Table -AutoSize
