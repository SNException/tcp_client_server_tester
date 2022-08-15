tcp_client_server_tester
------------------------

############################

!!! STILL IN DEVELOPMENT !!!
!!! PROCEED WITH CAUTION !!!

############################

Simple tool to test client-server tcp communications.

Currently at my work I quiet often use this tool for testing purposes:
    https://github.com/akshath/SocketTest

It has proven very useful to me but there were always some minor tweaks I would have liked.
So I decided to build my own version of it primarly customized to my own needs.

Some of the features and main differences:
    * No third party libraries
    * OpenJDK-17
    * MIT license
    * Hex view of the output/communication
    * Coloring of output/communication
    * UTF-8 by default
    * Build-In ascii table including char insertion from it
    * Repeat last command
    * Settings (e.g automatic stx-etx and/or new line insertions, customizable buffer size, ...etc)
    * Automatic and conditional responding of certain incoming messages (e.g if receive "POLL" then send "ACK")
    * Drag and drop file contents
    * IP and port validation
    * No UDP support (never needed it)

Requirements
------------

* OpenJDK 17.0.4 (on windows %JAVA_HOME% must be set, on linux /usr/bin/java* is used)


How to build and run
--------------------

Windows
    $ build.bat
    $ build.bat run

Other
    $ ./build.sh
    $ ./build.sh run


How to test
-----------

You can use the nc (netcat) on linux like this:

Client:
    $ nc 127.0.0.1 1234

Server:
    $ nc -l 1234


License
-------

Copyright (c) 2022 Niklas Schultz

Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
documentation files (the "Software"), to deal in the Software without restriction,
including without limitation the rights to use, copy, modify, merge, publish, distribute,
sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so,
subject to the following conditions:

The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED
TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
