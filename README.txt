tcp_client_server_tester
------------------------

##########################
!!!STILL IN DEVELOPMENT!!!
##########################




Simple tool to test client-server tcp communications.

Currently at my work I quiet often use this tool for testing purposes:
    https://github.com/akshath/SocketTest

It has proven very useful to me but there were always some minor tweaks I would have liked.
So I decided to build my own version of it.

Major differences:
    1.)  No third party libraries
    2.)  OpenJDK-17
    3.)  MIT license
    4.)  Hex view
    5.)  UTF-8 by default
    6.)  Build in ascii table
    7.)  Coloring of output
    8.)  History of last command
    9.)  Setings (e.g automatic stx-etx and/or new line insertions)
    10.) No UDP support (never needed it)


How to build
------------

Windows
    $ build.bat

Other
    $ ./build.sh


How to test
------------

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
