#!/usr/bin/env python

from place_and_route import PlaceCallerVPR
import os
import subprocess

###############
# Set options #
###############

architecture = 'benchmarks/k6_frac_N10_mem32K_40nm.xml'
circuits_folder = 'benchmarks/'
circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'

options = []
num_random_seeds = 1


#####################
# Place all ciruits #
#####################
os.chdir('..')
print('Compiling...')
subprocess.call(['./compile.sh'])

place_caller = PlaceCallerVPR(architecture, circuits_folder, circuits)
place_caller.place_all(options, num_random_seeds)
place_caller.save_results('place_vpr.csv')
