#!/usr/bin/env python

import os
from place_and_route import VPRPlaceCaller

base_folder = 'benchmarks/'
input_folder = '10fle/'
architecture = 'k6_frac_N10_mem32K_40nm'

os.chdir('..')
place_caller = VPRPlaceCaller(base_folder)


output_folder = '10fle_place-wld_vpr/'
place_caller.place_all(architecture, input_folder, output_folder, ['--timing_tradeoff', '0'])


output_folder = '10fle_place-td_vpr/'
place_caller.place_all(architecture, input_folder, output_folder, ['--timing_tradeoff', '0.5'])
