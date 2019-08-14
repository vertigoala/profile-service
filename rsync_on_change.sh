#!/bin/bash
#
# sync remote workspace into local path (remote IDE sessions like VS Code)
#
while true
do 
  rsync -rlptzv --progress --delete --exclude=.git --exclude out --exclude target "ala-builder:/home/ubuntu/projetos/profile-service/*" .
  sleep 5
done

