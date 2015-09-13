#!/usr/bin/env python3

import call
import sys, os
import re
import csv

#circuits = 'LU32PEEng LU8PEEng bgm blob_merge boundtop ch_intrinsics diffeq1 diffeq2 mcml mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
#circuits = 'blob_merge boundtop ch_intrinsics diffeq1 diffeq2 mkDelayWorker32B mkPktMerge mkSMAdapter4B or1200 raygentop sha stereovision0 stereovision1 stereovision2 stereovision3'
circuits = 'alu4 apex2 apex4 bigkey clma des diffeq dsip elliptic ex1010 ex5p frisc misex3 pdc s298 s38417 s38584.1 seq spla tseng'

circuit_list = circuits.split(' ')

output_path = sys.argv[1]
_file = open(output_path, 'w')
csv_file = csv.writer(_file)
csv_file.writerow(['benchmark', 'SA place time', 'SA WL cost', 'MDP place time', 'MDP WL cost'])

os.chdir('..')


for circuit in circuit_list:
    options = {
        '--input': 'benchmarks/Blif',
        '--circuit': circuit,
        '--architecture': '4lut',
        '--placer': 'SA;MDP',
    }

    flags = [
        '--pack',
        '--random',
        'inner_num=1'
    ]


    # Call the java placer
    print('Placing {0}'.format(circuit))
    out, err = call.placer(options, flags)

    # If there is an error: print it and exit
    if err:
        print('There was a problem while placing circuit "{0}":'.format(circuit))
        print(err)
        sys.exit(1)

    SA_time, SA_cost = call.get_stats(out, "SA")
    MDP_time, MDP_cost = call.get_stats(out, "MDP")

    row = [circuit, SA_time, SA_cost, MDP_time, MDP_cost]

    csv_file.writerow(row)
