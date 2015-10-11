import subprocess
import os
import re

def call(command):
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    # Return output
    out, err = p.communicate()
    return out.decode('utf-8'), err.decode('utf-8')


def placer(options, placer_options):

    command = build_place_command(options, placer_options)

    # Call the placer
    p = subprocess.Popen(command, stdout=subprocess.PIPE, stderr=subprocess.PIPE)

    # Return output
    out, err = p.communicate()
    return out.decode('utf-8'), err.decode('utf-8')


def build_place_command(options, placer_options):
    # Basic command
    command = [
        'java',
        '-cp',
        'bin:dependencies/args4j-2.32.jar:dependencies/json-simple-1.1.1.jar',
        'cli.CLI'
    ]

    # Add optional arguments
    for option in options:
        command += [option, options[option]]

    for option in placer_options:
        command.append('='.join((option, placer_options[option])))

    return command



def get_place_stats(output, prefix):
    regex = r'{0}\s+time:\s+(?P<time>[0-9.]+).*{0}\s+BB cost:\s+(?P<wl_cost>[0-9.]+).*{0}\s+max delay:\s+(?P<delay>[0-9.]+)'.format(prefix)
    return get_stats(output, regex)

def get_route_stats(output):
    regex = r'Total wirelength: (?P<wl_cost>\d+),.*Final critical path: (?P<delay>[0-9.]+) ns.*Routing took (?P<time>[0-9.]+) seconds'
    return get_stats(output, regex)

def get_stats(output, regex):
    pattern = re.compile(regex, re.DOTALL)
    match = pattern.search(output)

    time = match.group('time')
    wl_cost = match.group('wl_cost')
    delay = match.group('delay')

    return (time, wl_cost, delay)
