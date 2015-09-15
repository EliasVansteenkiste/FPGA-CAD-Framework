import subprocess
import os
import re

def placer(options, flags):

    # Basic command
    command = [
        'java',
        '-cp',
        'bin:dependencies/args4j-2.32.jar',
        'cli.CLI'
    ]

    # Add optional arguments
    for option in options:
        command += [option, options[option]]

    for flag in flags:
        command.append(flag)


    # Call the placer
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    # Return output
    out, err = p.communicate()
    return out.decode('utf-8'), err.decode('utf-8')


def get_stats(output, prefix):
    regex = r'{0}\s+place time:\s+(?P<time>[0-9.]+).*{0}\s+total cost:\s+(?P<cost>[0-9.]+)'.format(prefix)
    pattern = re.compile(regex, re.DOTALL)
    match = pattern.search(output)

    time = match.group('time')
    cost = match.group('cost')

    return (time, cost)
