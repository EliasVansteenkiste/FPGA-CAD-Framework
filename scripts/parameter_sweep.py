#!/usr/bin/env python

from place_and_route import ParameterSweeper
import os
import subprocess

###############
# Set options #
###############

architecture = 'benchmarks/k6_frac_N10_mem32K_40nm.xml'
circuits_folder = 'benchmarks/'
#circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'bgm mcml LU32PEEng LU8PEEng stereovision2'

fixed_options = ['--placer', 'wld_ap']
variable_options = {
    '--anchor_weight': [0.05, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6],
    '--anchor_weight_multiplier': [1.05, 1.1, 1.15, 1.2, 1.3],
}

num_random_seeds = 4

##########################################
# Place all ciruits and sweep parameters #
##########################################
os.chdir('..')
subprocess.call(['./compile.sh'])
sweeper = ParameterSweeper(architecture, circuits_folder, circuits)
sweeper.sweep(fixed_options, variable_options, num_random_seeds)
sweeper.save_results('sweep.csv')
