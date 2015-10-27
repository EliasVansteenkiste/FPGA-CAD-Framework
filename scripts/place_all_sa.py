#!/usr/bin/env python

import os
from place_and_route import PlaceCaller

base_folder = 'benchmarks/'
architecture = '10fle'
placer = 'td_sa'
options = ['timing_tradeoff=0.5']

os.chdir('..')
place_caller = PlaceCaller(base_folder)



place_caller.place_all(architecture, placer)
