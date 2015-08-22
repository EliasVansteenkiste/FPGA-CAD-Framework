#!/usr/bin/env python3

import call

options = {
    'input': 'benchmarks/vtr_benchmarks_netlist',
    'output': 'benchmarks/vtr_benchmarks_place',
    'circuit': 'boundtop',
    'placer': 'SA+AP',
}

out, err = call.placer(options)

print('Output:')
print(out)

print('Error:')
print(err)
