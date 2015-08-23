#!/usr/bin/env python3

import call
import sys, os
import re
import csv

#circuits = 'LU32PEEng LU8PEEng bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'



circuit_list = circuits.split(' ')
_file = open('sa_ap.csv', 'w')
csv_file = csv.writer(_file)
csv_file.writerow(['benchmark', 'cost after SA', 'cost after AP'])

os.chdir('..')


for circuit in circuit_list:
    options = {
        'input': '../../benchmarks/k6_N1_90nm_heterogeneous',
        'circuit': circuit,
        'placer': 'SA+AP',
    }

    # Call the java placer
    print('Placing {0}'.format(circuit))
    out, err = call.placer(options)

    # If there is an error: print it and exit
    if err:
        print('There was a problem while placing circuit "{0}":'.format(circuit))
        print(err)
        sys.exit(1)

    regex = re.compile(r'after SA\s+total cost:\s+(?P<SA>[0-9.]+).*after AP\s+total cost:\s+(?P<AP>[0-9.]+)', re.DOTALL)
    match = regex.search(out)

    SA = match.group('SA')
    AP = match.group('AP')

    csv_file.writerow([circuit, SA, AP])
