#!/usr/bin/env python

from place_and_route import PlaceCaller
import os

###############
# Set options #
###############

architecture = 'benchmarks/k6_frac_N10_mem32K_40nm.xml'
circuits_folder = 'benchmarks/'
circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'stereovision3 ch_intrinsics'
options = ['--placer', 'wld_gp']
results_file = 'test.csv'

#####################
# Place all ciruits #
#####################
os.chdir('..')
place_caller = PlaceCaller(architecture, circuits_folder, circuits)
place_caller.place_all(options)
place_caller.save_results(results_file)
