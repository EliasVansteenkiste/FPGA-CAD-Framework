#!/usr/bin/env python

import os
from place_and_route import PlaceCaller

base_folder = 'benchmarks/'
os.chdir('..')


architecture = '10fle'
placer = 'wld_ap'
options = ['solve_mode=complete', 'starting_anchor_weight=0', 'anchor_weight_increase=0.1']
#options = ['solve_mode=gradient', 'starting_anchor_weight=0', 'anchor_weight_increase=0.005', 'gradient_iterations=40', 'gradient_speed=0.4']
place_caller = PlaceCaller(base_folder)
place_caller.place_all(architecture, placer, options)