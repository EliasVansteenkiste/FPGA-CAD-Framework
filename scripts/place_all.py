#!/usr/bin/env python

from place_and_route import PlaceCaller
import os
import subprocess

###############
# Set options #
###############

architecture = 'benchmarks/k6_frac_N10_mem32K_40nm.xml'
circuits_folder = 'benchmarks/'
#circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'bgm mcml LU32PEEng LU8PEEng stereovision2 stereovision1'

# options = [
#     ['--placer', 'wld_ap'],
#     ['--placer', 'td_ap'],
#     ['--placer', 'wld_gp'],
#     ['--placer', 'td_gp', '--criticality_exponent', '4']
# ]
options = [
    ['--placer', 'td_gp']
]
num_random_seeds = 8

#####################
# Place all ciruits #
#####################
os.chdir('..')
subprocess.call(['./compile.sh'])

for i in range(len(options)):
    print('options: ' + ' '.join(options[i]))
    place_caller = PlaceCaller(architecture, circuits_folder, circuits)
    place_caller.place_all(options[i], num_random_seeds)
    place_caller.save_results('place{0}.csv'.format(i))
