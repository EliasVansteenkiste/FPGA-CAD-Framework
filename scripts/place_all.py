#!/usr/bin/env python3

import call
import sys, os
import re
import csv

circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'

placer = 'wld_ap'
input_folder = 'benchmarks/10fle'
output_folder = input_folder + '_place-' + placer
architecture = 'architectures/10fle.json'
start = 'net'

placer_options = {
}




options = {
    '--input': input_folder,
    '--output': output_folder,
    '--architecture': architecture,
    '--placer': placer,
    '--start': start,
}


os.chdir('..')

# Open output file
if not os.path.exists(output_folder):
    os.mkdir(output_folder)
_file = open(output_folder + '/place: ' + placer + '.csv', 'w')
csv_file = csv.writer(_file)

# Write header
command = ' '.join(call.build_place_command(options, placer_options))
csv_file.writerow(['','','','',command])
csv_file.writerow(['benchmark', 'place time', 'WL cost', 'max delay'])


circuit_list = circuits.split(' ')
for circuit in circuit_list:
    options['--circuit'] = circuit

    # Call the java placer
    print('Placing {0}'.format(circuit))
    out, err = call.call(call.build_place_command(options, placer_options))

    # If there is an error: print it and exit
    if err:
        print('There was a problem while placing circuit "{0}":'.format(circuit))
        print(err)
        sys.exit(1)

    time, wl_cost, max_delay = call.get_place_stats(out, placer)

    row = [circuit, time, wl_cost, max_delay]

    csv_file.writerow(row)
    _file.flush()

_file.close()
