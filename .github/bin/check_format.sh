#!/bin/sh

# make sure git has no un commit files
if [ -n "$(git status --untracked-files=no --porcelain)" ]; then
   echo "Please check format for files:"
   git status --untracked-files=no --porcelain
   exit -1
fi