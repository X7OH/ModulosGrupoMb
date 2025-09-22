@echo off

REM ************************************************************************************
REM * Copyright (C) 2012 Openbravo S.L.U.
REM * Licensed under the Openbravo Commercial License version 1.0
REM * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
REM * or in the legal folder of this module distribution.
REM ************************************************************************************

set DIRNAME=%~dp0

set CP="%DIRNAME%cpext/;%DIRNAME%poshw.jar"
dir /b "%DIRNAME%libext\*.jar" > %TEMP%\poshwlibs.tmp
FOR /F %%I IN (%TEMP%\poshwlibs.tmp) DO CALL "%DIRNAME%addcp.bat" "%DIRNAME%libext\%%I"

start /B javaw -cp %CP% -Dswing.defaultlaf=com.sun.java.swing.plaf.windows.WindowsLookAndFeel -Djava.util.logging.config.file="%DIRNAME%logging.properties" -Djava.library.path="%DIRNAME%lib/win32;%DIRNAME%lib/jacob" -Ddirname.path="%DIRNAME%./" com.openbravo.poshw.Main %1
