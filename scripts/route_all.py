#!/usr/bin/env python3

import call
import sys, os
import re
import csv
import glob

# Flexible config
input_folder = 'benchmarks/10fle'

# Fixed config
circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'LU32PEEng'
placer = sys.argv[1]
output_folder = input_folder + '_place-' + placer

# Switch to root dir
os.chdir('..')

# Get the architecture file
arch_files = glob.glob(input_folder + '/*.xml')
if len(arch_files) != 1:
    print('Less or more than 1 architecture file found in input folder')
    sys.exit(1)
arch_file = arch_files[0]

# Create the output folder
if not os.path.exists(output_folder):
    os.mkdir(output_folder)

# Open the output csv file and write the header
_file = open(output_folder + '/route: ' + placer + '.csv', 'w')
csv_file = csv.writer(_file)

csv_file.writerow([placer])
csv_file.writerow(['benchmark', 'route time', 'WL cost', 'max delay'])

# Loop through all circuits
circuit_list = circuits.split(' ')
for circuit in circuit_list:

    # Build the command
    command = [
        '../../vtr-modified/vpr/vpr',
        arch_file,
        circuit,
        '--route',
        '--blif_file', input_folder + '/' + circuit + '.blif',
        '--net_file', input_folder + '/' + circuit + '.net',
        '--place_file', output_folder + '/' + circuit + '.place',
        '--route_file', output_folder + '/' + circuit + '.route',
    ]

    # Call the VPR router
    print('Routing from {0}: {1}'.format(placer, circuit))
    out, err = call.call(command)
    print(out)
    # Remove temporary files
    os.remove(circuit + '.critical_path.out')
    os.remove(circuit + '.slack.out')
    os.remove('vpr_stdout.log')

    # Get and print statistics
    time, wl_cost, max_delay = call.get_route_stats(out)

    row = [circuit, time, wl_cost, max_delay]

    csv_file.writerow(row)
    _file.flush()

_file.close()
