#!/usr/bin/env python3

import call
import sys, os
import re
import csv

# Flexible config
input_folder = 'benchmarks/10fle'
architecture = 'architectures/10fle.json'
start = 'net'

placer_options = {
}


# Fixed config
circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
placer = sys.argv[1]
output_folder = input_folder + '_place-' + placer

options = {
    '--input': input_folder,
    '--output': output_folder,
    '--architecture': architecture,
    '--placer': placer,
    '--start': start,
}

# Switch to root dir
os.chdir('..')

# Create output folder
if not os.path.exists(output_folder):
    os.mkdir(output_folder)

# Open the output csv file and write the header
_file = open(output_folder + '/place: ' + placer + '.csv', 'w')
csv_file = csv.writer(_file)

command = ' '.join(call.build_place_command(options, placer_options))
csv_file.writerow(['','','','',command])
csv_file.writerow(['benchmark', 'place time', 'WL cost', 'max delay'])

# Loop through all circuits
circuit_list = circuits.split(' ')
for circuit in circuit_list:
    options['--circuit'] = circuit

    # Call the java placer
    print('Placing with {0}: {1}'.format(placer, circuit))
    out, err = call.call(call.build_place_command(options, placer_options))

    # If there is an error: print it and exit
    if err:
        print('There was a problem while placing circuit "{0}":'.format(circuit))
        print(err)
        sys.exit(1)

    # Get and print statistics
    time, wl_cost, max_delay = call.get_place_stats(out, placer)

    row = [circuit, time, wl_cost, max_delay]

    csv_file.writerow(row)
    _file.flush()

_file.close()
