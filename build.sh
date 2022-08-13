#!/bin/bash

src_dir=src
out_dir=bin
compile_flags="-J-Xms2048m -J-Xmx2048m -J-XX:+UseG1GC -Xdiags:verbose -Xlint:all -deprecation -Xmaxerrs 5 -encoding UTF8 --release 17 -g"

entry_point=Main
jvm_flags="-ea -Xms2048m -Xmx2048m -XX:+AlwaysPreTouch -XX:+UseG1GC -Xmixed"
possible_program_args="$2 $3 $4 $5 $6 $7 $8 $9"

if [ "$1" = "run" ]; then
    "/usr/bin/java" $jvm_flags -cp $out_dir $entry_point $possible_program_args
else
    if test -d $out_dir; then rm -r $out_dir;mkdir $out_dir; fi

    find $src_dir -type f > sources.txt
    "/usr/bin/javac" $compile_flags -d $out_dir -sourcepath $src_dir @sources.txt

    if [ $? -eq 0 ]
    then
      echo "Build successful"
    else
      echo "Build failed"
    fi

    rm sources.txt
fi
