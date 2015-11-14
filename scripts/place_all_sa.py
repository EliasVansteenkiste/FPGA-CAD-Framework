#!/usr/bin/env python

import os
from place_and_route import PlaceCaller, VPRPlaceCaller

base_folder = 'benchmarks/'
os.chdir('..')


architecture = '10fle'
placer = 'wld_sa'
place_caller = PlaceCaller(base_folder)
#place_caller.place_all(architecture, placer)



architecture = 'k6_frac_N10_mem32K_40nm'
input_folder = '10fle/'
output_folder = '10fle_place-wld_vpr/'
vpr_caller = VPRPlaceCaller(base_folder)
vpr_caller.place_all(architecture, input_folder, output_folder, ['--place_algorithm', 'bounding_box'])