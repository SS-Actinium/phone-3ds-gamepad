' Launch Hinge Pad with no console. Avoids the Windows stuck-Ctrl bug from cmd start/exit.
Option Explicit
Dim fso, sh, root, pyw, launcher
Set fso = CreateObject("Scripting.FileSystemObject")
Set sh = CreateObject("WScript.Shell")
root = fso.GetParentFolderName(WScript.ScriptFullName)
pyw = root & "\pc-server\.venv\Scripts\pythonw.exe"
launcher = root & "\pc-server\launcher.py"

If fso.FileExists(pyw) And fso.FileExists(launcher) Then
  sh.CurrentDirectory = root & "\pc-server"
  sh.Run """" & pyw & """ """ & launcher & """", 0, False
Else
  sh.Run "cmd /c """ & root & "\scripts\first-run-setup.cmd""", 1, True
End If
