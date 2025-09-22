#!/bin/sh

# ************************************************************************************
# * Copyright (C) 2012 Openbravo S.L.U.
# * Licensed under the Openbravo Commercial License version 1.0
# * You may obtain a copy of the License at http://www.openbravo.com/legal/obcl.html
# * or in the legal folder of this module distribution.
# ************************************************************************************

DIRNAME=`dirname $0`

# Select the library folder
case "`uname -s`" in
Linux)
    case "`uname -m`" in
    i686) LIBRARYPATH=lib/i686-pc-linux-gnu;;
    x86_64|amd64) LIBRARYPATH=lib/x86_64-unknown-linux-gnu;;
    esac;;
Darwin) LIBRARYPATH=lib/mac-10.5;;
CYGWIN*|MINGW32*) LIBRARYPATH=lib/win32;;
esac

CLASSPATH=$DIRNAME/cpext:$DIRNAME/poshw.jar
for f in `find "$DIRNAME/libext" -type f -name "*.jar"`
do
  CLASSPATH=$CLASSPATH:$f
done

java -cp $CLASSPATH -Djava.util.logging.config.file=$DIRNAME/logging.properties -Djava.library.path=$DIRNAME/$LIBRARYPATH -Ddirname.path=$DIRNAME/ com.openbravo.poshw.Main "$@"
