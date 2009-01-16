#!/bin/bash
#
# Copyright 2009 Google Inc. All Rights Reserved.
# Author: rayc@google.com (Ray Colline#)

if [ -e $1 ]; then
  /bin/cp $1 $2
fi
