#!/usr/bin/env python3

import call
import sys, os
import re
import csv

#circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'LU32PEEng LU8PEEng mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'


circuit_list = circuits.split(' ')
_file = open('ap.csv', 'w')
csv_file = csv.writer(_file)
csv_file.writerow(['benchmark', 'place time', 'cost', 'delay'])

os.chdir('..')

options = {
    '--input': 'benchmarks/heterogeneous',
    '--architecture': 'heterogeneous',
    '--placer': 'AP',
    '--start': 'net',
}

placer_options = {

}

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

    (ap_time, ap_cost, ap_delay) = call.get_stats(out, "ap")

    row = [circuit, ap_time, ap_cost, ap_delay]

    csv_file.writerow(row)
    #print(row)
