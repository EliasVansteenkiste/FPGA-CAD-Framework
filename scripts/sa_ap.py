#!/usr/bin/env python3

import call
import sys, os
import re
import csv

circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'

input_folder = 'benchmarks/1ble'
output_folder = 'benchmarks/1ble_place'
architecture = 'heterogeneous'
placer = 'td_sa'
start = 'place'
random = False

placer_options = {
    'detailed': '1',
    'Rlim': '5',
    'greedy': '1',
    'effort_level': '3',
}





options = {
    '--input': input_folder,
    '--output': output_folder,
    '--architecture': architecture,
    '--placer': placer,
    '--start': start,
}

if random:
    options['--random'] = ''


circuit_list = circuits.split(' ')
_file = open(placer + '.csv', 'w')
csv_file = csv.writer(_file)

command = ' '.join(call.build_command(options, placer_options))
csv_file.writerow(['','','','',command])
csv_file.writerow(['benchmark', 'place time', 'cost', 'delay'])

os.chdir('..')

for circuit in circuit_list:
    options['--circuit'] = circuit

    # Call the java placer
    print('Placing {0}'.format(circuit))
    out, err = call.placer(options, placer_options)

    # If there is an error: print it and exit
    if err:
        print('There was a problem while placing circuit "{0}":'.format(circuit))
        print(err)
        sys.exit(1)

    (ap_time, ap_cost, ap_delay) = call.get_stats(out, placer)

    row = [circuit, ap_time, ap_cost, ap_delay]

    csv_file.writerow(row)
    _file.flush()

_file.close()
