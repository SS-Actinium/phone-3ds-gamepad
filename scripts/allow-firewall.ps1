# Run in an elevated PowerShell window.
# Opens inbound UDP 26760 for the default Hinge Pad port.
New-NetFirewallRule -DisplayName "Hinge Pad UDP 26760" `
    -Direction Inbound -Protocol UDP -LocalPort 26760 -Action Allow
Write-Output "Inbound UDP 26760 allowed."
